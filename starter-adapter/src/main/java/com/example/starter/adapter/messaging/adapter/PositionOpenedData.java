package com.example.starter.adapter.messaging.adapter;

import java.time.Instant;
import java.util.UUID;

record PositionOpenedData(UUID positionId, String isin, Instant openedAt) {}
