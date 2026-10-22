package com.example.starter.adapter.messaging.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.starter.adapter.messaging.properties.KafkaMessagingProperties;
import com.example.starter.domain.model.event.PositionOpened;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaEventPublisherTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(KafkaEventPublisher.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attach() {
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
        logger.setAdditive(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void anAsyncSendFailureNeverReachesTheCaller() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(anyString(), any(), anyString()))
                .thenReturn(CompletableFuture.<SendResult<String, String>>failedFuture(
                        new TimeoutException("broker down")));
        KafkaEventPublisher publisher =
                new KafkaEventPublisher(template, new KafkaMessagingProperties("positions"), "sample-api");

        assertThatCode(() -> publisher.publish(opened())).doesNotThrowAnyException();

        assertThat(appender.list).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void aSynchronousFailureIsLoggedWithItsStackAndNeverRethrown() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(anyString(), any(), anyString())).thenThrow(new IllegalStateException("closed"));
        KafkaEventPublisher publisher =
                new KafkaEventPublisher(template, new KafkaMessagingProperties("positions"), "sample-api");

        assertThatCode(() -> publisher.publish(opened())).doesNotThrowAnyException();

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNotNull();
        });
    }

    private static PositionOpened opened() {
        return new PositionOpened(UUID.randomUUID(), "US0378331005", Instant.parse("2026-10-22T10:00:00Z"));
    }
}
