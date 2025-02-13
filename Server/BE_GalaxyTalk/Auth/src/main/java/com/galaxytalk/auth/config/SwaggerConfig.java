package com.galaxytalk.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${gateway.service.url}")
    private String gatewayServiceUrl;


    // 추가 서버 정보 설정
    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .addServersItem(new Server().url(authServiceUrl).description("Auth Service"))
                .components(new Components())
                .servers(List.of(new Server().url(gatewayServiceUrl).description("Gateway Server")))
                .servers(List.of(new Server().url("https://i12a503.p.ssafy.io/gateway").description("Gateway with https Server")));

    }

    // CORS 설정을 위한 WebMvcConfigurer 추가
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/v3/api-docs/**")
                        .allowedOrigins(new String[]{gatewayServiceUrl, "https://i12a503.p.ssafy.io/**"})
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);

                registry.addMapping("/swagger-ui/**")
                        .allowedOrigins(new String[]{gatewayServiceUrl, "https://i12a503.p.ssafy.io/**"})
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
                registry.addMapping("/webjars/**")
                        .allowedOrigins(new String[]{gatewayServiceUrl, "https://i12a503.p.ssafy.io/**"})
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}