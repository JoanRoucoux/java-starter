package com.example.starter.cucumber;

import io.cucumber.java.Before;

public class Hooks {
    @Before
    public void resetMarketData() {
        CucumberSpringConfiguration.MARKET_DATA.resetAll();
    }
}
