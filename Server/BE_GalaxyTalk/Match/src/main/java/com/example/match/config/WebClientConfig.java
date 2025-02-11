package com.example.match.config;

import jakarta.annotation.PostConstruct;
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
    @Value("${ai.service.url}")
    private String aiServiceBaseUrl;

    @Value("${auth.service.url}")  // 서비스 이름 주입
    private String authServiceName;

    @Value("${chat.service.url}")  // 서비스 이름 주입
    private String chatServiceName;

    @PostConstruct
    public void init() {
        log.info("Initialized WebClient with chat service name: {}", chatServiceName);
        log.info("Initialized WebClient with auth service name: {}", authServiceName);
    }

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
                .baseUrl("http://" + chatServiceName)
                .build();
    }

    @Bean
    public WebClient authServiceClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("http://" + authServiceName)
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
