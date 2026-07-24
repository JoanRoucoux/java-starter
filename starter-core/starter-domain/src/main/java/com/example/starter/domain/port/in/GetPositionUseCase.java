package com.example.starter.domain.port.in;

import com.example.starter.domain.model.Position;
import java.util.Optional;
import java.util.UUID;

/** Inbound port: read a position back. */
public interface GetPositionUseCase {

    Optional<Position> byId(UUID id);
}
