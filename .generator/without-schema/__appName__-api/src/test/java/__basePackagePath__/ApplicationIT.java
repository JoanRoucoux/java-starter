package {{basePackage}};

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Boots the full application: no database is involved (this application was generated without the
 * schema module), so this only checks that the context starts and that security answers 401
 * without ever contacting an identity provider.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoadsAndRejectsUnauthenticatedRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity("/quote/US0378331005", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void exposesTheHealthProbeWithoutAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
