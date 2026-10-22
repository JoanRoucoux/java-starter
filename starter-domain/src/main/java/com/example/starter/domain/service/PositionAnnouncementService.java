package com.example.starter.domain.service;

import com.example.starter.domain.model.Position;
import com.example.starter.domain.model.event.PositionOpened;
import com.example.starter.domain.port.in.CreatePositionUseCase;
import com.example.starter.domain.port.out.PublishEventPort;
import java.math.BigDecimal;

public class PositionAnnouncementService implements CreatePositionUseCase {

    private final CreatePositionUseCase delegate;
    private final PublishEventPort publishEvent;

    public PositionAnnouncementService(CreatePositionUseCase delegate, PublishEventPort publishEvent) {
        this.delegate = delegate;
        this.publishEvent = publishEvent;
    }

    @Override
    public Position create(String isin, BigDecimal quantity) {
        Position position = delegate.create(isin, quantity);
        publishEvent.publish(new PositionOpened(position.id(), position.isin(), position.createdAt()));
        return position;
    }
}
