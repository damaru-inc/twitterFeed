package com.damaru.twitterfeed.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document("tweets")
public class MongoTweet {

    public MongoTweet(String id, String username, String source, String topic, Instant createdAt,
                      String message, int score) {
        this.id = id;
        this.username = username;
        this.source = source;
        this.topic = topic;
        this.createdAt = createdAt;
        this.message = message;
        this.score = score;
    }

    @Id
    private String id;
    private String username;
    private String source;
    private Instant createdAt;
    private String message;
    private String topic;
    private int score;
}
