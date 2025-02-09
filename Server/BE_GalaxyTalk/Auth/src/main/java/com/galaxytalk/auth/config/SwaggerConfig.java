package com.galaxytalk.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@OpenAPIDefinition(
        // API가 실행되는 기본 서버의 URL 설정

        servers = {
                @io.swagger.v3.oas.annotations.servers.Server(url = "http://localhost:8080", description = "Gateway Server")

}
)

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
                .components(new Components());
    }

    // CORS 설정을 위한 WebMvcConfigurer 추가
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/v3/api-docs/**")
                        .allowedOrigins(gatewayServiceUrl)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);

                registry.addMapping("/swagger-ui/**")
                        .allowedOrigins(gatewayServiceUrl)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}