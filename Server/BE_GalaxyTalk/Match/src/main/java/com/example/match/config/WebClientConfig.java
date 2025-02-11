package com.example.match.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
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
    @Value("${ai.ip.url}")
    private static String aiServiceBaseUrl;
    @Value("${chat.service.url}")
    private static String chatServiceBaseUrl;
    @Value("${auth.service.url}")
    private static String authServiceBaseUrl;

    @Bean
    @LoadBalanced // Eureka 기반 로드밸런싱 적용
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse());
    }

    @Bean
    public WebClient aiServiceClientWithoutLoadBalancing() {
        return WebClient.builder()
                .baseUrl(aiServiceBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    @Bean
    public WebClient chatServiceClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(chatServiceBaseUrl) // Eureka 서비스명 사용
                .build();
    }

    @Bean
    public WebClient authServiceClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(authServiceBaseUrl) // Eureka 서비스명 사용
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
