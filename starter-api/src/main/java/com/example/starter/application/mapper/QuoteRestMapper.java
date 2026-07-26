package com.example.starter.application.mapper;

import com.example.starter.domain.model.Quote;
import com.example.starter.generated.model.QuoteResponse;
import org.springframework.stereotype.Component;

/** Maps the domain model to the generated DTOs. One mapper per resource — never a shared one. */
@Component
public class QuoteRestMapper {

    public QuoteResponse toResponse(Quote quote) {
        QuoteResponse response = new QuoteResponse();
        response.setIsin(quote.isin());
        response.setPrice(quote.price());
        return response;
    }
}
