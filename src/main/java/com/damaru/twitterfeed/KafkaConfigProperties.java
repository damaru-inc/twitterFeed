package com.damaru.twitterfeed;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaConfigProperties {

    private String bootstrapServers;
}
