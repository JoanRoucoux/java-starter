package com.example.starter.kafka.consumer;

import com.example.starter.domain.port.in.GetPositionUseCase;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PositionOpenedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PositionOpenedConsumer.class);
    private static final String POSITION_OPENED = "position.opened";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final GetPositionUseCase getPosition;

    public PositionOpenedConsumer(GetPositionUseCase getPosition) {
        this.getPosition = getPosition;
    }

    @KafkaListener(
            id = "positions-worker",
            topics = "${app.messaging.kafka.positions-topic:positions}",
            groupId = "positions-worker")
    public void onMessage(String payload) {
        UUID positionId;
        try {
            JsonNode envelope = JSON_MAPPER.readTree(payload);
            if (!POSITION_OPENED.equals(envelope.path("type").asString(null))) {
                return;
            }
            positionId =
                    UUID.fromString(envelope.path("data").path("positionId").asString());
        } catch (RuntimeException malformed) {
            log.warn("skipping an unreadable envelope on positions", malformed);
            return;
        }

        getPosition
                .byId(positionId)
                .ifPresentOrElse(
                        position -> log.info("position opened: {} on {}", position.id(), position.isin()),
                        () -> log.warn("position opened event for an unknown position: {}", positionId));
    }
}
