package com.damaru.twitterfeed;

import com.twitter.clientlib.model.Tweet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, Tweet tweet) {
        String msg = tweet.toJson();
        String id = tweet.getId();
        //log.info("Sending message {} to {}", id, topic);
        kafkaTemplate.send(topic, id, msg);
    }
}
