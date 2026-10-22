package com.example.starter.adapter.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.starter.adapter.messaging.adapter.KafkaEventPublisher;
import com.example.starter.adapter.messaging.properties.KafkaMessagingProperties;
import com.example.starter.domain.model.event.PositionOpened;
import com.example.starter.domain.port.out.PublishEventPort;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaMessagingConfigTest {

    private static final KafkaMessagingProperties PROPERTIES = new KafkaMessagingProperties("positions");

    @Test
    void wiresAKafkaEventPublisher() {
        MockProducer<String, String> mockProducer =
                new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());

        PublishEventPort port = new KafkaMessagingConfig()
                .publishEventPort(new KafkaTemplate<>(() -> mockProducer), PROPERTIES, "sample-api");

        assertThat(port).isInstanceOf(KafkaEventPublisher.class);
    }

    @Test
    void theWiredPublisherSendsToTheConfiguredTopic() {
        MockProducer<String, String> mockProducer =
                new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        PublishEventPort port = new KafkaMessagingConfig()
                .publishEventPort(new KafkaTemplate<>(() -> mockProducer), PROPERTIES, "sample-api");

        port.publish(new PositionOpened(UUID.randomUUID(), "US0378331005", Instant.parse("2026-10-22T10:00:00Z")));

        ProducerRecord<String, String> sent = mockProducer.history().getFirst();
        assertThat(sent.topic()).isEqualTo("positions");
        assertThat(sent.value()).contains("\"type\":\"position.opened\"");
    }
}
