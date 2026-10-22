package com.example.starter.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.starter.domain.model.Position;
import com.example.starter.domain.model.event.PositionOpened;
import com.example.starter.domain.port.in.CreatePositionUseCase;
import com.example.starter.domain.port.out.PublishEventPort;
import com.example.starter.domain.service.PositionAnnouncementService;
import com.example.starter.domain.service.PositionService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PositionEventsConfigTest {

    @Test
    void decoratesThePositionServiceAndAnnouncesWhatItCreates() {
        PositionService positionService = mock(PositionService.class);
        PublishEventPort publishEvent = mock(PublishEventPort.class);
        Position position = Position.open("US0378331005", BigDecimal.TEN, new BigDecimal("123.45"));
        when(positionService.create("US0378331005", BigDecimal.TEN)).thenReturn(position);

        CreatePositionUseCase useCase =
                new PositionEventsConfig().announcingCreatePosition(positionService, publishEvent);

        assertThat(useCase).isInstanceOf(PositionAnnouncementService.class);
        assertThat(useCase.create("US0378331005", BigDecimal.TEN)).isSameAs(position);
        verify(publishEvent).publish(new PositionOpened(position.id(), position.isin(), position.createdAt()));
    }
}
