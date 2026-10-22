package com.example.starter.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TopicsConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TopicsConfig.class);

    @Test
    void declaresThePositionsTopicByDefault() {
        runner.run(context -> {
            NewTopic topic = context.getBean(NewTopic.class);

            assertThat(topic.name()).isEqualTo("positions");
            assertThat(topic.numPartitions()).isEqualTo(1);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry(TopicConfig.RETENTION_MS_CONFIG, "604800000");
        });
    }

    @Test
    void readsTheTopicNameFromTheSamePropertyAsTheListenerAndThePublisher() {
        runner.withPropertyValues("app.messaging.kafka.positions-topic=custom-positions")
                .run(context ->
                        assertThat(context.getBean(NewTopic.class).name()).isEqualTo("custom-positions"));
    }
}
