package com.example.starter.infrastructure.config;

import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@FunctionalInterface
public interface EndpointAuthorization {
    void authorize(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry requests);
}
