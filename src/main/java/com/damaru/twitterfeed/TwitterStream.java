package com.damaru.twitterfeed;

import com.google.common.reflect.TypeToken;
import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.JSON;
import com.twitter.clientlib.TwitterCredentialsBearer;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.Place;
import com.twitter.clientlib.model.StreamingTweet;
import com.twitter.clientlib.model.Tweet;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class TwitterStream implements ApplicationContextAware {

    @Autowired
    TwitterConfigProperties twitterConfigProperties;

    @Autowired
    EventProducer eventProducer;

    private ApplicationContext context;

    private static final int MAX_HITS = 10_000;
    private int hits;
    private int misses;
    private Instant startTime;
    private Instant endTime;

    static final Set<String> PLACE_FILTER_WORDS = Set.of(
            "canada",
            "calgary",
            "edmonton",
            "halifax",
            "montreal",
            "ottawa",
            "st. john's",
            "toronto",
            "winnipeg",
            "vancouver");

    static final Set<String> POLITICS_FILTER_WORDS_1 = Set.of(
            "#cdnpoli",
            "#onpoli",
            "trudeau",
            "bergen",
            "pollievre",
            "fullerton",
            "convoy",
            "freedom",
            "canada day",
            "satan"
            );

    static final Set<String> POLITICS_FILTER_WORDS = Set.of(
            "convoy",
            "candayconvoy",
            "canadadayconvoy",
            "ottawa",
            "canada day"
    );


    public void run() {
        startTime = Instant.now();
        FilteredTopic locations = new FilteredTopic("twitter-locations", PLACE_FILTER_WORDS);
        FilteredTopic politics = new FilteredTopic("twitter-politics", POLITICS_FILTER_WORDS);
        List<FilteredTopic> filteredTopics = List.of(politics);
        //List<FilteredTopic> filteredTopics = List.of(locations, politics);
        //TwitterCredentialsBearer credentials = new TwitterCredentialsBearer(System.getenv("TWITTER_BEARER_TOKEN"));
        TwitterCredentialsBearer credentials = new TwitterCredentialsBearer(twitterConfigProperties.getBearerToken());
        TwitterApi apiInstance = new TwitterApi();
        apiInstance.setTwitterCredentials(credentials);
        // Set the params values
        Set<String> expansions = Set.of("author_id");
        Set<String> tweetFields = Set.of("text", "created_at", "lang", "author_id", "source");
        Set<String> userFields = Set.of("location", "name");
        Set<String> mediaFields = Set.of();
        Set<String> placeFields = Set.of("full_name", "country");
        Set<String> pollFields = Set.of();
        Integer backfillMinutes = 0; // Integer | The number of minutes of backfill requested
        try {
            InputStream result = apiInstance.tweets()
                    .sampleStream(expansions, tweetFields, userFields, mediaFields, placeFields, pollFields,
                            backfillMinutes);
            try {
                JSON json = new JSON();
                Type localVarReturnType = new TypeToken<StreamingTweet>() {
                }.getType();
                BufferedReader reader = new BufferedReader(new InputStreamReader(result));
                String line = reader.readLine();
                while (line != null) {
                    StreamingTweet streamingTweet = json.getGson().fromJson(line, localVarReturnType);
                    if (streamingTweet != null) {
                        if (applyFilters(streamingTweet, filteredTopics)) {
                            hits++;
                        } else {
                            misses++;
                        }
                        if (hits == MAX_HITS) {
                            shutdown(filteredTopics);
                        }
                    }
                    line = reader.readLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
                shutdown(filteredTopics);
            }
        } catch (
                ApiException e) {
            System.err.println("Exception when calling TweetsApi#sampleStream");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            shutdown(filteredTopics);
        }

    }

    private boolean applyFilters(StreamingTweet streamingTweet, List<FilteredTopic> filterTopics) {
        boolean ret = false;
        for (FilteredTopic filterTopic : filterTopics) {
            int score = filter(streamingTweet, filterTopic.getFilterStrings());
            if (score > 1) {
                filterTopic.incrementCount();
                //log.info("{}: {}", filterTopic.getCount(), filterTopic.getTopic());
                ret = true;
                eventProducer.send(filterTopic.getTopic(), streamingTweet, score);
            }
        }
        return ret;
    }

    int filter(StreamingTweet streamingTweet, Set<String> filterStrings) {
        int score = 0;
        Tweet tweet = streamingTweet.getData();
        if (tweet != null) {
            if ("en".equals(tweet.getLang())) {
                String content = tweet.getText().toLowerCase();
                for (String filterWord : filterStrings) {
                    if (content.contains(filterWord)) {
                        score++;
                    }
                }
            }
        }

        return score;
    }

    private void shutdown(List<FilteredTopic> filterTopics) {
        endTime = Instant.now();
        for (FilteredTopic filteredTopic : filterTopics) {
            log.info(filteredTopic.toString());
        }
        Duration dur = Duration.between(startTime, endTime);
        log.info("Shutting down. hits: {} misses: {} duration: {}:{}:{}",
                hits, misses,
                dur.toHoursPart(), dur.toMinutesPart(), dur.toSecondsPart());
        ((ConfigurableApplicationContext) context).close();
        System.exit(0);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    //region oldstuff
    public void runa() throws Exception {

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setApplicationOnlyAuthEnabled(true);
        builder.setOAuthConsumerKey(twitterConfigProperties.getApiKey())
                .setOAuthConsumerSecret(twitterConfigProperties.getApiSecretKey());
        Twitter twitter = TwitterFactory.getSingleton();
        AccessToken accessToken = new AccessToken(twitterConfigProperties.getApiKey(), twitterConfigProperties.getApiSecretKey());
        //OAuth2Token token = twitter.getOAuth2Token(builder);
        //twitter.setOAuthConsumerKey("[consumer key]", "[consumer secret]");
        //twitter.setOAuthAccessToken(configProperties.getBearerToken());
        //OAuth2Token token = new OAuth2Token("Bearer", configProperties.getBearerToken());
        //twitter.setOAuth2Token(token);

        List<Status> statuses = twitter.getHomeTimeline();
        System.out.println("Showing home timeline.");
        for (Status status : statuses) {
            System.out.println(status.getUser().getName() + ":" +
                    status.getText());
        }
    }

    public void runnew() throws Exception {
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/sample/stream");

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setHeader("Authorization", String.format("Bearer %s", twitterConfigProperties.getBearerToken()));

        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        if (null != entity) {
            BufferedReader reader = new BufferedReader(new InputStreamReader((entity.getContent())));
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
        }
    }
//endregion

}
