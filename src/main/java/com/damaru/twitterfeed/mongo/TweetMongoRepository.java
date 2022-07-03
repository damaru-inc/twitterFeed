package com.damaru.twitterfeed.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TweetMongoRepository extends MongoRepository<MongoTweet, String> {

}
