package com.galaxytalk.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SwaggerAggregationConfig는 Gateway에서 Aggregated Swagger UI 관련 추가 설정을 담당합니다.
 * 본 예제에서는 기본 설정 외에 Gateway 자체의 API 그룹핑을 위한 Bean을 정의합니다.
 */
@Configuration
public class SwaggerAggregationConfig {

    /**
     * Gateway 내에서 별도로 그룹화할 API가 있을 경우 설정 (옵션)
     */
    @Bean
    public GroupedOpenApi gatewayApi() {
        return GroupedOpenApi.builder()
                .group("gateway")
                .pathsToMatch("/**")
                .build();
    }
}
