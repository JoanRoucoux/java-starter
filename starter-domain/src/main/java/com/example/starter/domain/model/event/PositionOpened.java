package com.example.starter.domain.model.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PositionOpened(UUID positionId, String isin, Instant openedAt) implements DomainEvent {

    public PositionOpened {
        Objects.requireNonNull(positionId, "positionId");
        Objects.requireNonNull(isin, "isin");
        Objects.requireNonNull(openedAt, "openedAt");
    }
}
