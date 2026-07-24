package com.example.starter.infrastructure.config;

import com.example.starter.domain.port.out.LoadPositionPort;
import com.example.starter.domain.port.out.MarketDataPort;
import com.example.starter.domain.port.out.SavePositionPort;
import com.example.starter.domain.service.PositionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the hexagon: the domain services are plain Java classes,
 * wired here against the ports implemented by the adapters.
 */
@Configuration(proxyBeanMethods = false)
class DomainConfig {

    @Bean
    PositionService positionService(
            SavePositionPort savePositionPort, LoadPositionPort loadPositionPort, MarketDataPort marketDataPort) {
        return new PositionService(savePositionPort, loadPositionPort, marketDataPort);
    }
}
