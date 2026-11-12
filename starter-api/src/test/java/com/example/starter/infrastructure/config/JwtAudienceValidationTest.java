package com.example.starter.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtAudienceValidationTest {
    @TempDir
    static Path directory;

    private static KeyPair keys;
    private static Path publicKey;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(keys.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        publicKey = Files.writeString(directory.resolve("public.pem"), pem);
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
            .withPropertyValues(
                    "spring.security.oauth2.resourceserver.jwt.public-key-location=file:"
                            + publicKey.toUri().getPath(),
                    "spring.security.oauth2.resourceserver.jwt.audiences=local-api");

    @Test
    void acceptsATokenIssuedForTheConfiguredAudience() {
        runner.run(context -> {
            JwtDecoder decoder = context.getBean(JwtDecoder.class);
            assertThat(decoder.decode(token("local-api")).getAudience()).containsExactly("local-api");
        });
    }

    @Test
    void rejectsATokenSignedWithTheRightKeyForAnotherAudience() {
        runner.run(context -> {
            JwtDecoder decoder = context.getBean(JwtDecoder.class);
            assertThatThrownBy(() -> decoder.decode(token("another-api"))).isInstanceOf(JwtValidationException.class);
        });
    }

    private static String token(String audience) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user")
                .audience(audience)
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) keys.getPrivate()));
        return jwt.serialize();
    }
}
