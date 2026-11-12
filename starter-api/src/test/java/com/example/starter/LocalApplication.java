package com.example.starter;

import org.springframework.boot.SpringApplication;

public class LocalApplication {
    public static void main(String[] args) {
        System.setProperty("spring.liquibase.enabled", "false");
        SpringApplication.from(Application::main).with(OpenSecurityConfig.class).run(args);
    }
}
