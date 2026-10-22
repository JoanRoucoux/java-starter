package com.example.starter.adapter.client.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.starter.adapter.client.properties.MarketDataClientProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class MarketDataClientConfigTest {

    private static final WireMockServer server =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @BeforeAll
    static void startServer() {
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    void buildsARestClientOnTheConfiguredBaseUrlAndTimeouts() {
        server.stubFor(get(urlEqualTo("/quotes/US0378331005")).willReturn(ok("reached")));
        MarketDataClientProperties properties =
                new MarketDataClientProperties(server.baseUrl(), Duration.ofSeconds(2), Duration.ofSeconds(5));

        RestClient restClient = new MarketDataClientConfig().marketDataRestClient(RestClient.builder(), properties);

        assertThat(restClient
                        .get()
                        .uri("/quotes/{isin}", "US0378331005")
                        .retrieve()
                        .body(String.class))
                .isEqualTo("reached");
    }

    @Test
    void retriesATransientServerError() {
        server.resetAll();
        server.stubFor(get(urlEqualTo("/quotes/US0378331005"))
                .inScenario("flaky")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("recovered")
                .willReturn(aResponse().withStatus(503)));
        server.stubFor(get(urlEqualTo("/quotes/US0378331005"))
                .inScenario("flaky")
                .whenScenarioStateIs("recovered")
                .willReturn(ok("reached")));
        MarketDataClientProperties properties =
                new MarketDataClientProperties(server.baseUrl(), Duration.ofSeconds(2), Duration.ofSeconds(5));

        RestClient restClient = new MarketDataClientConfig().marketDataRestClient(RestClient.builder(), properties);

        assertThat(restClient
                        .get()
                        .uri("/quotes/{isin}", "US0378331005")
                        .retrieve()
                        .body(String.class))
                .isEqualTo("reached");
    }
}
