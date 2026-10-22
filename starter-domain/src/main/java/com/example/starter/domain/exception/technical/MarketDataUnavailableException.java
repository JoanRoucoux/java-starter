package com.example.starter.domain.exception.technical;

public class MarketDataUnavailableException extends TechnicalException {
    public MarketDataUnavailableException(String isin, Throwable cause) {
        super("Market data unavailable for instrument: " + isin, cause);
    }
}
