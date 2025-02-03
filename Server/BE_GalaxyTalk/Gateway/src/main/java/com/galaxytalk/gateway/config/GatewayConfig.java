package com.galaxytalk.gateway.config;

import com.galaxytalk.gateway.filter.CookieFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, CookieFilter cookieFilter) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/oauth/**")
                        .filters(f -> f.filter(cookieFilter)) // 쿠키 기반 필터 적용
                        .uri("http://localhost:8081"))
                .route("oauth-login", r -> r.path("/oauth2/authorization/naver")
                        .uri("http://localhost:8081"))
                .build();
    }
}
