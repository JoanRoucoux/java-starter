package com.example.starter.domain.port.in;

import com.example.starter.domain.model.Position;

public interface RevaluePositionUseCase {
    Position revalue(Position position);
}
