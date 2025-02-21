package com.galaxytalk.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig는 Auth 서비스의 Swagger API 문서에 사용될 메타 정보를 설정합니다.
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI Bean을 생성하여 Swagger UI에 제목, 버전, 설명을 표시합니다.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version("1.0")
                        .description("Documentation for Auth Service"));
    }
}
