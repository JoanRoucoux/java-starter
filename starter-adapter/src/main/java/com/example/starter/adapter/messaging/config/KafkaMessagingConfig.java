package com.example.starter.adapter.messaging.config;

import com.example.starter.adapter.messaging.adapter.KafkaEventPublisher;
import com.example.starter.adapter.messaging.properties.KafkaMessagingProperties;
import com.example.starter.domain.port.out.PublishEventPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaMessagingProperties.class)
class KafkaMessagingConfig {

    @Bean
    PublishEventPort publishEventPort(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaMessagingProperties properties,
            @Value("${spring.application.name}") String applicationName) {
        return new KafkaEventPublisher(kafkaTemplate, properties, applicationName);
    }
}
