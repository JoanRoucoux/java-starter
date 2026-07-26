package com.example.starter.domain.service;

import com.example.starter.domain.exception.business.UnknownInstrumentException;
import com.example.starter.domain.model.Position;
import com.example.starter.domain.port.in.CreatePositionUseCase;
import com.example.starter.domain.port.in.GetPositionUseCase;
import com.example.starter.domain.port.out.LoadPositionPort;
import com.example.starter.domain.port.out.MarketDataPort;
import com.example.starter.domain.port.out.SavePositionPort;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Use case implementations. Plain Java: wired as beans by the application module. */
public class PositionService implements CreatePositionUseCase, GetPositionUseCase {

    private final SavePositionPort savePositionPort;
    private final LoadPositionPort loadPositionPort;
    private final MarketDataPort marketDataPort;

    public PositionService(
            SavePositionPort savePositionPort, LoadPositionPort loadPositionPort, MarketDataPort marketDataPort) {
        this.savePositionPort = savePositionPort;
        this.loadPositionPort = loadPositionPort;
        this.marketDataPort = marketDataPort;
    }

    @Override
    public Position create(String isin, BigDecimal quantity) {
        BigDecimal price = marketDataPort.currentPrice(isin).orElseThrow(() -> new UnknownInstrumentException(isin));
        return savePositionPort.save(Position.open(isin, quantity, price));
    }

    @Override
    public Optional<Position> byId(UUID id) {
        return loadPositionPort.findById(id);
    }
}
