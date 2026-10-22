package com.example.starter.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;

class TopicsConfigTest {

    @Test
    void declaresThePositionsTopic() {
        NewTopic topic = new TopicsConfig().positionsTopic();

        assertThat(topic.name()).isEqualTo("positions");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
        assertThat(topic.configs()).containsEntry(TopicConfig.RETENTION_MS_CONFIG, "604800000");
    }
}
