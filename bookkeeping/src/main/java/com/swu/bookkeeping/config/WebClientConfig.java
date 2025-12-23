package com.swu.bookkeeping.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${currencylist.api.path}")
    private String currencyListPath;

    @Value("${currencyexchange.api.path}")
    private String currencyExchangePath;

    @Bean
    public WebClient currencyListWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(currencyListPath)
               .defaultHeaders(headers -> {
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        })
                .build();
    }

    @Bean
    public WebClient currencyExchangeWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(currencyExchangePath)
               .defaultHeaders(headers -> {
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        })
                .build();
    }



}
