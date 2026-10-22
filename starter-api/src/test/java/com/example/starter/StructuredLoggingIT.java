package com.example.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.starter.domain.port.out.MarketDataPort;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "spring.liquibase.change-log=classpath:db/changelog/changelog-master.xml",
            "logging.structured.format.console=ecs"
        })
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @MockitoBean
    private MarketDataPort marketDataPort;

    @Test
    void writesTheUseCaseLineAsEcsJsonWithATraceId(CapturedOutput output) throws Exception {
        when(marketDataPort.currentPrice(any())).thenReturn(Optional.of(new BigDecimal("123.45")));

        mockMvc.perform(get("/quote/{isin}", "US0378331005").with(jwt())).andExpect(status().isOk());

        JsonNode line = Arrays.stream(output.getOut().split("\\R"))
                .filter(candidate -> candidate.contains("\"useCase\":\"GetQuote\""))
                .map(candidate -> JsonMapper.shared().readTree(candidate))
                .findFirst()
                .orElseThrow();

        assertThat(line.path("log").path("logger").asString()).isEqualTo("com.example.starter.usecase");
        assertThat(line.path("outcome").asString()).isEqualTo("success");
        assertThat(line.path("durationMs").isNumber()).isTrue();
        assertThat(line.path("service").path("name").asString())
                .isEqualTo(environment.getProperty("spring.application.name"));
        assertThat(line.path("traceId").asString()).hasSize(32);
    }
}
