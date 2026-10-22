package com.example.starter.domain.service;

import com.example.starter.domain.exception.business.UnknownInstrumentException;
import com.example.starter.domain.model.Position;
import com.example.starter.domain.port.in.RevaluePositionUseCase;
import com.example.starter.domain.port.out.MarketDataPort;
import java.math.BigDecimal;

public class RevaluePositionService implements RevaluePositionUseCase {
    private final MarketDataPort marketDataPort;

    public RevaluePositionService(MarketDataPort marketDataPort) {
        this.marketDataPort = marketDataPort;
    }

    @Override
    public Position revalue(Position position) {
        BigDecimal price = marketDataPort
                .currentPrice(position.isin())
                .orElseThrow(() -> new UnknownInstrumentException(position.isin()));
        return position.revalue(price);
    }
}
