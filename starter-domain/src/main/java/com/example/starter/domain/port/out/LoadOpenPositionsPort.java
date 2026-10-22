package com.example.starter.domain.port.out;

import com.example.starter.domain.model.Position;
import java.util.List;

public interface LoadOpenPositionsPort {
    List<Position> findOpen();
}
