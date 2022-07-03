package com.damaru.twitterfeed.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
@Profile("kafka")
public class KafkaConfigProperties {

    private String bootstrapServers;
}
