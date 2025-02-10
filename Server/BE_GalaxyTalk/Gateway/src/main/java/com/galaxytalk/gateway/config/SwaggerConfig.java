package com.galaxytalk.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${gateway.service.url}")
    private String gatewayServiceUrl;

    //자기꺼 등록
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("MSA API Gateway")
                        .version("1.0")
                        .description("모든 서비스의 API 명세 통합"))
                .addServersItem(new Server().url(gatewayServiceUrl).description("Gateway"))
                .components(new Components());
    }

    // 하위 그룹 등록
    @Bean
    public GroupedOpenApi oauthServiceApi() {
        return GroupedOpenApi.builder()
                .group("OAuth Service")
                .pathsToMatch("/api/oauth/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("OAuth API")))
                .build();
    }
    @Bean
    public GroupedOpenApi supportServiceApi() {
        return GroupedOpenApi.builder()
                .group("Support Service")
                .pathsToMatch("/api/support/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Support API")))
                .build();
    }
    @Bean
    public GroupedOpenApi chatServiceApi() {
        return GroupedOpenApi.builder()
                .group("Chat Service")
                .pathsToMatch("/api/chat/**")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Chat API")))
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