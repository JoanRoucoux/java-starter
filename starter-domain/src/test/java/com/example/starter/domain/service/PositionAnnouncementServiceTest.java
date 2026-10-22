package com.example.starter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.starter.domain.exception.business.UnknownInstrumentException;
import com.example.starter.domain.model.Position;
import com.example.starter.domain.model.event.DomainEvent;
import com.example.starter.domain.model.event.PositionOpened;
import com.example.starter.domain.port.in.CreatePositionUseCase;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionAnnouncementServiceTest {

    private static final String ISIN = "US0378331005";

    @Test
    void announcesTheOpenedPositionAfterTheDelegateCreatedIt() {
        List<DomainEvent> published = new ArrayList<>();
        Position position = Position.open(ISIN, BigDecimal.TEN, new BigDecimal("123.45"));
        CreatePositionUseCase delegate = (isin, quantity) -> position;

        Position created = new PositionAnnouncementService(delegate, published::add).create(ISIN, BigDecimal.TEN);

        assertThat(created).isSameAs(position);
        assertThat(published).singleElement().isEqualTo(new PositionOpened(position.id(), ISIN, position.createdAt()));
    }

    @Test
    void announcesNothingWhenTheDelegateRefuses() {
        List<DomainEvent> published = new ArrayList<>();
        CreatePositionUseCase delegate = (isin, quantity) -> {
            throw new UnknownInstrumentException(isin);
        };

        assertThatThrownBy(() -> new PositionAnnouncementService(delegate, published::add).create(ISIN, BigDecimal.ONE))
                .isInstanceOf(UnknownInstrumentException.class);
        assertThat(published).isEmpty();
    }
}
