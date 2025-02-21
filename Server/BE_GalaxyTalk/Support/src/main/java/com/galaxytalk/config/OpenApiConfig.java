package com.galaxytalk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI Bean을 생성하여 Swagger UI에 제목, 버전, 설명을 표시합니다.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Support Service API")
                        .version("1.0")
                        .description("Documentation for Support Service"));
    }
}
