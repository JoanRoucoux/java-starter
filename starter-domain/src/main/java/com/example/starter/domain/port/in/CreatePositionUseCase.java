package com.example.starter.domain.port.in;

import com.example.starter.domain.model.Position;
import java.math.BigDecimal;

public interface CreatePositionUseCase {
    Position create(String isin, BigDecimal quantity);
}
