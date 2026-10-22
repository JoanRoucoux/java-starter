package com.example.starter.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;

public interface MarketDataPort {
    Optional<BigDecimal> currentPrice(String isin);
}
