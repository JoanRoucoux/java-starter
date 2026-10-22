package com.example.starter.batch.config;

import com.example.starter.domain.port.out.MarketDataPort;
import com.example.starter.domain.service.RevaluePositionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BatchDomainConfig {
    @Bean
    RevaluePositionService revaluePositionService(MarketDataPort marketDataPort) {
        return new RevaluePositionService(marketDataPort);
    }
}
