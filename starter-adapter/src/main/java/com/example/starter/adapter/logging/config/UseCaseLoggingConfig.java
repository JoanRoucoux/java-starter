package com.example.starter.adapter.logging.config;

import com.example.starter.adapter.logging.adapter.UseCaseLoggingPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class UseCaseLoggingConfig {

    @Bean
    static UseCaseLoggingPostProcessor useCaseLoggingPostProcessor() {
        return new UseCaseLoggingPostProcessor();
    }
}
