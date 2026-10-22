package com.example.starter.adapter.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.messaging.kafka")
public record KafkaMessagingProperties(
        @DefaultValue("positions") String positionsTopic) {}
