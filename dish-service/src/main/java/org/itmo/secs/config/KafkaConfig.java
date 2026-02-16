package org.itmo.secs.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic itemTopic() {
        return TopicBuilder.name("item-events").build();
    }
    @Bean
    public NewTopic dishTopic() {
        return TopicBuilder.name("dish-events").build();
    }
}
