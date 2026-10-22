package com.example.starter.domain.port.out;

import com.example.starter.domain.model.Position;

public interface SavePositionPort {
    Position save(Position position);
}
