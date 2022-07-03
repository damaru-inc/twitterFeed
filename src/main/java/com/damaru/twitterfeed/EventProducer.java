package com.damaru.twitterfeed;

import com.damaru.twitterfeed.mongo.MongoTweet;
import com.damaru.twitterfeed.mongo.TweetMongoRepository;
import com.twitter.clientlib.model.StreamingTweet;
import com.twitter.clientlib.model.Tweet;
import com.twitter.clientlib.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
public class EventProducer {

    //private final KafkaTemplate<String, String> kafkaTemplate;
    private final TweetMongoRepository tweetMongoRepository;

//    public EventProducer(KafkaTemplate<String, String> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }

    public EventProducer(TweetMongoRepository tweetMongoRepository) {
        this.tweetMongoRepository = tweetMongoRepository;
    }

    public void send(String topic, StreamingTweet streamingTweet, int score) {
        Tweet tweet = streamingTweet.getData();
        String id = tweet.getId();
        String userName = getUsername(streamingTweet);
        String source = tweet.getSource();
        OffsetDateTime createdAt = tweet.getCreatedAt();
        Instant created = createdAt.toInstant();
        String text = tweet.getText();
        MongoTweet mt = new MongoTweet(id, userName, source, topic, created, text, score);
        log.info("Sending message {} to {}: {}: {}: {}", id, topic, userName, source, text);
        tweetMongoRepository.save(mt);
        //kafkaTemplate.send(topic, id, msg);
    }

    private String getUsername(StreamingTweet streamingTweet) {
        List<User> users = streamingTweet.getIncludes().getUsers();
        Tweet tweet = streamingTweet.getData();
        String authorId = tweet.getAuthorId();
        String userName = null;
        for (User user : users) {
            if (authorId.equals(user.getId())) {
                userName = user.getUsername();
            }
        }
        return userName;
    }


}
