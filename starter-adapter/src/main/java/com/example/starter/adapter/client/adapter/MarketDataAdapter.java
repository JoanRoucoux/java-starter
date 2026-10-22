package com.example.starter.adapter.client.adapter;

import com.example.starter.domain.exception.technical.MarketDataUnavailableException;
import com.example.starter.domain.port.out.MarketDataPort;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class MarketDataAdapter implements MarketDataPort {
    private final RestClient restClient;

    MarketDataAdapter(RestClient marketDataRestClient) {
        this.restClient = marketDataRestClient;
    }

    @Override
    public Optional<BigDecimal> currentPrice(String isin) {
        try {
            MarketDataQuote quote =
                    restClient.get().uri("/quotes/{isin}", isin).retrieve().body(MarketDataQuote.class);
            return Optional.ofNullable(quote).map(MarketDataQuote::price);
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (RestClientException failure) {
            throw new MarketDataUnavailableException(isin, failure);
        }
    }

    record MarketDataQuote(String isin, BigDecimal price) {}
}
