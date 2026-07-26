package com.example.starter.domain.port.in;

import com.example.starter.domain.model.Quote;

/** Inbound port: read the current price of an instrument. */
public interface GetQuoteUseCase {

    Quote byIsin(String isin);
}
