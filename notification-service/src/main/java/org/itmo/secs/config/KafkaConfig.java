package org.itmo.secs.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, ""
        ));
    }

    @Bean
    public NewTopic itemEventsTopic() {
        return TopicBuilder.name("item-events").build();
    }

    @Bean
    public NewTopic dishEventsTopic() {
        return TopicBuilder.name("dish-events").build();
    }

    @Bean
    public NewTopic menuEventsTopic() {
        return TopicBuilder.name("menu-events").build();
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name("user-events").build();
    }
}
