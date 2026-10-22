package com.example.starter.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.starter.domain.model.Position;
import com.example.starter.domain.port.in.GetPositionUseCase;
import com.example.starter.domain.port.out.LoadPositionPort;
import com.example.starter.domain.port.out.MarketDataPort;
import com.example.starter.domain.port.out.SavePositionPort;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkerDomainConfigTest {

    @Test
    void wiresThePositionLookupOverItsPorts() {
        LoadPositionPort loadPosition = mock(LoadPositionPort.class);
        Position position = Position.open("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        when(loadPosition.findById(position.id())).thenReturn(Optional.of(position));

        GetPositionUseCase useCase = new WorkerDomainConfig()
                .getPositionUseCase(mock(SavePositionPort.class), loadPosition, mock(MarketDataPort.class));

        assertThat(useCase.byId(position.id())).contains(position);
    }
}
