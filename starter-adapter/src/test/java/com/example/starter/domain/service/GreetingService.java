package com.example.starter.domain.service;

import com.example.starter.domain.port.in.GreetUseCase;
import com.example.starter.domain.port.in.WaveUseCase;

public class GreetingService implements GreetUseCase, WaveUseCase {

    @Override
    public String greet(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("blank name");
        }
        return "hello " + name;
    }

    @Override
    public void wave() {}

    public String notAPortMethod() {
        return "plain";
    }
}
