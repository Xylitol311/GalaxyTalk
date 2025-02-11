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
    public HttpHeadersFilter customXForwardedHeadersFilter() {  // 이름 변경
        return new XForwardedHeadersFilter() {
            @Override
            public HttpHeaders filter(HttpHeaders headers, ServerWebExchange exchange) {
                headers.remove("X-Forwarded-For");
                headers.remove("X-Forwarded-Proto");
                headers.remove("X-Forwarded-Host");
                headers.remove("X-Forwarded-Port");
                return super.filter(headers, exchange);
            }
        };
    }

}
