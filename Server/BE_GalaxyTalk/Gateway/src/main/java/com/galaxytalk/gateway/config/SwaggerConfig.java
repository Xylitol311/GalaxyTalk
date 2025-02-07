package com.galaxytalk.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        servers = {
                @io.swagger.v3.oas.annotations.servers.Server(url = "http://localhost:8080", description = "Gateway Server")
        }
)
public class SwaggerConfig {


    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("MSA API Gateway")
                        .version("1.0")
                        .description("모든 서비스의 API 명세 통합"))
                .addServersItem(new Server().url("http://localhost:8080").description("Gateway"))
                .components(new Components());
    }

    @Bean
    public GroupedOpenApi oauthServiceApi() {
        return GroupedOpenApi.builder()
                .group("OAuth Service")
                .pathsToMatch("/api/oauth/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("OAuth API")))
                .build();
    }
    @Bean
    public GroupedOpenApi matchServiceApi() {
        return GroupedOpenApi.builder()
                .group("Match Service")
                .pathsToMatch("/api/match/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Match API")))
                .build();
    }
}