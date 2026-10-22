package com.example.starter.kafka.config;

import com.example.starter.domain.port.in.GetPositionUseCase;
import com.example.starter.domain.port.out.LoadPositionPort;
import com.example.starter.domain.port.out.MarketDataPort;
import com.example.starter.domain.port.out.SavePositionPort;
import com.example.starter.domain.service.PositionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class WorkerDomainConfig {

    @Bean
    GetPositionUseCase getPositionUseCase(
            SavePositionPort savePosition, LoadPositionPort loadPosition, MarketDataPort marketData) {
        return new PositionService(savePosition, loadPosition, marketData);
    }
}
