package com.galaxytalk.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class GlobalCorsConfig {

    @Value("${front.url}")
    private String frontUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 3000 포트에서 오는 요청을 허용
        configuration.setAllowedOrigins(Arrays.asList(frontUrl));
        configuration.addAllowedHeader("Content-Type");
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);  // preflight 응답 캐시 시간 설정 (초 단위)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로에 CORS 설정 적용

        // Swagger 경로에 대해 CORS 허용 추가
        source.registerCorsConfiguration("/swagger-ui/**", configuration);
        source.registerCorsConfiguration("/v3/api-docs/**", configuration);

        return source;
    }

    //cors에 대해 실제 필터링 작업해줌 corsConfigurationSource은 설정임
    @Bean
    public CorsWebFilter corsWebFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsWebFilter(corsConfigurationSource);
    }
}
