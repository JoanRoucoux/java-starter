package com.example.starter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.starter.domain.exception.business.UnknownInstrumentException;
import com.example.starter.domain.model.Position;
import com.example.starter.domain.model.PositionStatus;
import com.example.starter.domain.port.out.LoadPositionPort;
import com.example.starter.domain.port.out.MarketDataPort;
import com.example.starter.domain.port.out.SavePositionPort;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The whole domain tests without Spring: plain JUnit and Mockito against the ports. */
@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    private static final String ISIN = "US0378331005";

    @Mock
    private SavePositionPort savePositionPort;

    @Mock
    private LoadPositionPort loadPositionPort;

    @Mock
    private MarketDataPort marketDataPort;

    @Test
    void createValuesThePositionAtTheCurrentMarketPrice() {
        when(marketDataPort.currentPrice(ISIN)).thenReturn(Optional.of(new BigDecimal("123.45")));
        when(savePositionPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        PositionService service = new PositionService(savePositionPort, loadPositionPort, marketDataPort);
        Position position = service.create(ISIN, new BigDecimal("10"));

        assertThat(position.isin()).isEqualTo(ISIN);
        assertThat(position.quantity()).isEqualTo(new BigDecimal("10"));
        assertThat(position.price()).isEqualTo(new BigDecimal("123.45"));
        assertThat(position.status()).isEqualTo(PositionStatus.OPEN);
        assertThat(position.id()).isNotNull();
        assertThat(position.createdAt()).isNotNull();
    }

    @Test
    void createRejectsAnUnknownInstrument() {
        when(marketDataPort.currentPrice(ISIN)).thenReturn(Optional.empty());

        PositionService service = new PositionService(savePositionPort, loadPositionPort, marketDataPort);

        assertThatExceptionOfType(UnknownInstrumentException.class)
                .isThrownBy(() -> service.create(ISIN, BigDecimal.ONE))
                .withMessageContaining(ISIN);
        verifyNoInteractions(savePositionPort);
    }

    @Test
    void byIdReadsFromTheRepository() {
        UUID id = UUID.randomUUID();
        Position stored = Position.open(ISIN, BigDecimal.ONE, new BigDecimal("42"));
        when(loadPositionPort.findById(id)).thenReturn(Optional.of(stored));

        PositionService service = new PositionService(savePositionPort, loadPositionPort, marketDataPort);

        assertThat(service.byId(id)).contains(stored);
    }
}
