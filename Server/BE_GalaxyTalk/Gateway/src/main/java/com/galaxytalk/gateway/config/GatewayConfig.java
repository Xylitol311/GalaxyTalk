package com.galaxytalk.gateway.config;

import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

@Configuration
public class GatewayConfig {
    @Bean
    public HttpHeadersFilter xForwardedHeadersFilter() {
        return new XForwardedHeadersFilter() {
            @Override
            public HttpHeaders filter(HttpHeaders headers, ServerWebExchange exchange) {
                // 기존 헤더 제거
                headers.remove("X-Forwarded-For");
                headers.remove("X-Forwarded-Proto");
                headers.remove("X-Forwarded-Host");
                headers.remove("X-Forwarded-Port");
                // 부모 클래스의 filter 메서드 호출하고 결과 반환
                return super.filter(headers, exchange);
            }
        };
    }
}
