package com.example.starter.domain.port.in;

import com.example.starter.domain.model.Quote;

public interface GetQuoteUseCase {
    Quote byIsin(String isin);
}
