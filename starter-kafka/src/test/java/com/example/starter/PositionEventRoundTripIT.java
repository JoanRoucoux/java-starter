package com.example.starter;

import com.example.starter.domain.model.Position;
import com.example.starter.domain.model.event.PositionOpened;
import com.example.starter.domain.port.out.PublishEventPort;
import com.example.starter.domain.port.out.SavePositionPort;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "spring.liquibase.change-log=classpath:db/changelog/changelog-master.xml")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class PositionEventRoundTripIT {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(15);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.3.1");

    @Autowired
    private PublishEventPort publishEvent;

    @Autowired
    private SavePositionPort savePosition;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private ObservationRegistry observationRegistry;

    @Test
    void aPublishedPositionOpenedEventIsConsumedUnderTheSameTrace(CapturedOutput output) {
        awaitTheListenerToHaveAssignedPartitions();
        Position position = savePosition.save(Position.open("US0378331005", BigDecimal.TEN, BigDecimal.ONE));

        String traceId = Observation.createNotStarted("test-publish", observationRegistry)
                .observe(() -> {
                    publishEvent.publish(new PositionOpened(position.id(), position.isin(), position.createdAt()));
                    return tracer.currentSpan().context().traceId();
                });

        awaitUntil(
                () -> output.getOut()
                        .lines()
                        .anyMatch(line -> line.contains("position opened: " + position.id()) && line.contains(traceId)),
                "no consumer line carrying the publishing trace id");
    }

    private void awaitTheListenerToHaveAssignedPartitions() {
        MessageListenerContainer container = registry.getListenerContainer("positions-worker");
        awaitUntil(
                () -> container.getAssignedPartitions() != null
                        && !container.getAssignedPartitions().isEmpty(),
                "positions-worker never got a partition assignment");
    }

    private static void awaitUntil(BooleanSupplier condition, String failure) {
        long deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError(failure + " within " + POLL_TIMEOUT);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        }
    }
}
