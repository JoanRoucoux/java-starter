package com.example.starter.domain.port.out;

import com.example.starter.domain.model.Position;

/** Outbound port: write access to stored positions. */
public interface SavePositionPort {

    Position save(Position position);
}
