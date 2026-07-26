package com.example.starter.domain.port.out;

import com.example.starter.domain.model.Position;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: read access to stored positions. */
public interface LoadPositionPort {

    Optional<Position> findById(UUID id);
}
