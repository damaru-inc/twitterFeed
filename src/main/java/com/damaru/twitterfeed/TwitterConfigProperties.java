package com.damaru.twitterfeed;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "twitter.api")
public class TwitterConfigProperties {

    private String apiKey;
    private String apiSecretKey;
    private String bearerToken;

}
