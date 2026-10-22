package {{basePackage}}.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "app.security.permit-all=true")
public class CucumberSpringConfiguration {
    static final WireMockServer MARKET_DATA = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @DynamicPropertySource
    static void marketDataBaseUrl(DynamicPropertyRegistry registry) {
        if (!MARKET_DATA.isRunning()) {
            MARKET_DATA.start();
        }
        registry.add("app.client.market-data.base-url", MARKET_DATA::baseUrl);
    }
}
