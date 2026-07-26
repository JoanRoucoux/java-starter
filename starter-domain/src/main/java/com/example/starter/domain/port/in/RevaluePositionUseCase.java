package com.example.starter.domain.port.in;

import com.example.starter.domain.model.Position;

/** Inbound port: value a position again, at the price the market shows now. */
public interface RevaluePositionUseCase {

    Position revalue(Position position);
}
