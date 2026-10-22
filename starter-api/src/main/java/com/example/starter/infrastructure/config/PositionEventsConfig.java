package com.example.starter.infrastructure.config;

import com.example.starter.domain.port.in.CreatePositionUseCase;
import com.example.starter.domain.port.out.PublishEventPort;
import com.example.starter.domain.service.PositionAnnouncementService;
import com.example.starter.domain.service.PositionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
class PositionEventsConfig {

    @Bean
    @Primary
    CreatePositionUseCase announcingCreatePosition(PositionService positionService, PublishEventPort publishEvent) {
        return new PositionAnnouncementService(positionService, publishEvent);
    }
}
