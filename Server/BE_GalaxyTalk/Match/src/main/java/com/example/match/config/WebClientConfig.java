package com.example.match.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${chat.service.url}")
    private String chatServiceUrl;

    @Bean
    public WebClient aiServiceClient() {
        return WebClient.builder()
                .baseUrl(aiServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    @Bean
    public WebClient chatServiceClient() {
        return WebClient.builder()
                .baseUrl(chatServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
