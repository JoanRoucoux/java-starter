package com.example.starter.infrastructure.config;

import com.example.starter.domain.port.out.MarketDataPort;
import com.example.starter.domain.service.QuoteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class QuoteDomainConfig {
    @Bean
    QuoteService quoteService(MarketDataPort marketDataPort) {
        return new QuoteService(marketDataPort);
    }
}
