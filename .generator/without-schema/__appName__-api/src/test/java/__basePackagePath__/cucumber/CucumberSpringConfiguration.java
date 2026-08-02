package {{basePackage}}.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Shared Spring context for every scenario in this glue package (cucumber-spring wires it once
 * per run). No database is involved (this application was generated without the schema module),
 * so — unlike the reference implementation's version of this class — there is no Testcontainers
 * Postgres to start. Security is opened up: these scenarios test business behavior, not
 * authentication, which is already covered by the unit tests and ApplicationIT.
 *
 * <p>Cucumber requires glue classes to be public, unlike the rest of this test suite.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "app.security.permit-all=true")
public class CucumberSpringConfiguration {

    /** Stands in for the market data provider; step definitions stub it per scenario. */
    static final WireMockServer MARKET_DATA = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @DynamicPropertySource
    static void marketDataBaseUrl(DynamicPropertyRegistry registry) {
        if (!MARKET_DATA.isRunning()) {
            MARKET_DATA.start();
        }
        registry.add("app.client.market-data.base-url", MARKET_DATA::baseUrl);
    }
}
