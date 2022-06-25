package com.damaru.twitterfeed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class TwitterFeedApplication implements CommandLineRunner {

    @Autowired
    TwitterStream twitterStream;

    public static void main(String[] args) {
        SpringApplication.run(TwitterFeedApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        twitterStream.run();
    }
}
