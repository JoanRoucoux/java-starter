package com.example.starter.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration(proxyBeanMethods = false)
public class PositionSecurityConfig {
    @Bean
    EndpointAuthorization positionWriteAuthorization() {
        return requests ->
                requests.requestMatchers(HttpMethod.POST, "/position").hasAuthority("SCOPE_positions:write");
    }
}
