package com.galaxytalk.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

//게이트 웨이 설정파일
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/oauth/**")
                        .uri("lb://AUTH-SERVICE")) // 대문자 서비스 이름
                //http://localhost:8081
//                .route("oauth-login", r -> r.path("/oauth2/authorization/naver")
//                        .uri("lb://AUTH-SERVICE")) // 대문자 서비스 이름
                .route("match-service", r -> r.path("/api/match/**")
                        .uri("lb://MATCH-SERVICE")) // 대문자 서비스 이름
                .build();
    }
}