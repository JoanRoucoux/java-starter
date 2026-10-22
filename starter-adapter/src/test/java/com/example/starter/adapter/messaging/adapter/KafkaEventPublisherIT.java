package com.example.starter.adapter.messaging.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.starter.adapter.messaging.properties.KafkaMessagingProperties;
import com.example.starter.domain.model.event.PositionOpened;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class KafkaEventPublisherIT {

    private static final UUID POSITION = UUID.randomUUID();
    private static final String APPLICATION_NAME = "sample-api";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(10);
    private static final KafkaMessagingProperties PROPERTIES = new KafkaMessagingProperties("positions");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.3.1");

    private static JsonMapper json;
    private static DefaultKafkaProducerFactory<String, String> producerFactory;
    private static KafkaTemplate<String, String> kafkaTemplate;

    private KafkaEventPublisher publisher;
    private KafkaConsumer<String, String> consumer;

    @BeforeAll
    static void startBrokerAndCreateTopics() throws Exception {
        json = KafkaEventPublisher.eventMapper();
        producerFactory = new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
        try (Admin admin =
                Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(PROPERTIES.positionsTopic(), 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    @AfterAll
    static void closeTemplate() {
        producerFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        publisher = new KafkaEventPublisher(kafkaTemplate, PROPERTIES, APPLICATION_NAME);
        consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "kafka-event-publisher-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class));
        consumer.subscribe(List.of(PROPERTIES.positionsTopic()));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void publishesAPositionOpenedKeyedByPosition() {
        publisher.publish(new PositionOpened(POSITION, "US0378331005", Instant.parse("2026-10-22T10:00:00Z")));

        ConsumerRecord<String, String> record = pollMatching(value -> value.contains(POSITION.toString()));
        JsonNode envelope = json.readTree(record.value());
        JsonNode data = envelope.get("data");

        assertThat(record.key()).isEqualTo(POSITION.toString());
        assertThat(envelope.get("type").asText()).isEqualTo("position.opened");
        assertThat(envelope.get("version").asInt()).isEqualTo(1);
        assertThat(envelope.get("id").asText()).isNotBlank();
        assertThat(envelope.get("source").asText()).isEqualTo(APPLICATION_NAME);
        assertThatCode(() -> Instant.parse(envelope.get("occurredAt").asText())).doesNotThrowAnyException();
        assertThat(data.get("positionId").asText()).isEqualTo(POSITION.toString());
        assertThat(data.get("isin").asText()).isEqualTo("US0378331005");
        assertThat(data.has("quantity")).isFalse();
        assertThat(data.has("price")).isFalse();
    }

    @Test
    void neverThrowsWhenTheBrokerIsUnreachableEvenAcrossSeveralEvents() {
        DefaultKafkaProducerFactory<String, String> unreachableFactory = new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "127.0.0.1:1",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class,
                ProducerConfig.MAX_BLOCK_MS_CONFIG,
                250));
        try {
            KafkaEventPublisher unreachable =
                    new KafkaEventPublisher(new KafkaTemplate<>(unreachableFactory), PROPERTIES, APPLICATION_NAME);

            long start = System.nanoTime();
            assertThatCode(() -> {
                        for (int i = 0; i < 5; i++) {
                            unreachable.publish(new PositionOpened(UUID.randomUUID(), "US0378331005", Instant.now()));
                        }
                    })
                    .doesNotThrowAnyException();

            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        } finally {
            unreachableFactory.destroy();
        }
    }

    private ConsumerRecord<String, String> pollMatching(Predicate<String> valueMatches) {
        long deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> polled = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : polled) {
                if (valueMatches.test(record.value())) {
                    return record;
                }
            }
        }
        throw new AssertionError("no matching record polled on topic " + PROPERTIES.positionsTopic());
    }
}
