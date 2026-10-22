package com.example.starter.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.starter.domain.model.Position;
import com.example.starter.domain.port.in.GetPositionUseCase;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PositionOpenedConsumerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(PositionOpenedConsumer.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final List<UUID> looked = new ArrayList<>();
    private final Position position = Position.open("US0378331005", BigDecimal.TEN, BigDecimal.ONE);

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
    void aPositionOpenedEnvelopeIsLoggedOnce() {
        consumerKnowing(position).onMessage(envelope("position.opened", position.id()));

        assertThat(looked).containsExactly(position.id());
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).isEqualTo("position opened: " + position.id() + " on US0378331005");
        });
    }

    @Test
    void anUnknownPositionIsWarnedAbout() {
        consumerKnowing(null).onMessage(envelope("position.opened", position.id()));

        assertThat(appender.list)
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
    }

    @Test
    void anotherTypeIsIgnored() {
        consumerKnowing(position).onMessage(envelope("something.else", position.id()));

        assertThat(looked).isEmpty();
        assertThat(appender.list).isEmpty();
    }

    @Test
    void aMalformedMessageIsLoggedAndSkippedRatherThanBlockingThePartition() {
        consumerKnowing(position).onMessage("not json at all");

        assertThat(looked).isEmpty();
        assertThat(appender.list)
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
    }

    private PositionOpenedConsumer consumerKnowing(Position known) {
        GetPositionUseCase getPosition = id -> {
            looked.add(id);
            return Optional.ofNullable(known).filter(candidate -> candidate.id().equals(id));
        };
        return new PositionOpenedConsumer(getPosition);
    }

    private static String envelope(String type, UUID positionId) {
        return "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"type\":\"" + type + "\",\"version\":1,"
                + "\"occurredAt\":\"2026-10-22T10:00:00Z\",\"source\":\"sample-api\","
                + "\"data\":{\"positionId\":\"" + positionId
                + "\",\"isin\":\"US0378331005\",\"openedAt\":\"2026-10-22T10:00:00Z\"}}";
    }
}
