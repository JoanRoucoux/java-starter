package com.example.starter.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.starter.application.mapper.QuoteRestMapper;
import com.example.starter.domain.port.in.GetQuoteUseCase;
import com.example.starter.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuoteController.class)
@Import({SecurityConfig.class, QuoteRestMapper.class})
@TestPropertySource(properties = "app.security.permit-all=true")
@ActiveProfiles("local")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetQuoteUseCase getQuote;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void noPropertyOrProfileOpensTheApi() throws Exception {
        mockMvc.perform(get("/quote/{isin}", "US0378331005")).andExpect(status().isUnauthorized());
    }
}
