package com.cadac.stone_inscription.kafka.config;

import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.cadac.stone_inscription.kafka.registry.TopicRegistry;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic reportSubmittedTopic() {
        return TopicBuilder.name(TopicRegistry.REPORT_SUBMITTED)
                .partitions(12)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(TimeUnit.DAYS.toMillis(7)))
                .build();
    }

    @Bean
    public NewTopic reportSubmittedDltTopic() {
        return TopicBuilder.name(TopicRegistry.REPORT_SUBMITTED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
