package com.example.starter.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A holding of a quantity of an instrument, valued at the price seen when it was opened. */
public record Position(
        UUID id, String isin, BigDecimal quantity, BigDecimal price, PositionStatus status, Instant createdAt) {

    public Position {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(isin, "isin");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Position open(String isin, BigDecimal quantity, BigDecimal price) {
        return new Position(UUID.randomUUID(), isin, quantity, price, PositionStatus.OPEN, Instant.now());
    }
}
