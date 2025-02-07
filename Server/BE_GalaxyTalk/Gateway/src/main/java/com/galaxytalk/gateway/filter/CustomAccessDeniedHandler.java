package com.galaxytalk.gateway.filter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galaxytalk.gateway.dto.ApiResponseDto;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;

public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 직렬화를 위한 ObjectMapper


    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // ApiResponseDto를 사용한 응답 데이터 생성
        ApiResponseDto apiResponseDto = ApiResponseDto.forbidden;

        CustomErrorResponse errorResponse = new CustomErrorResponse(403, apiResponseDto.forbidden);

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    // JSON 응답 구조를 맞추기 위한 내부 클래스
    private static class CustomErrorResponse {
        private final int statusCode;
        private final ApiResponseDto data;

        public CustomErrorResponse(int statusCode, ApiResponseDto data) {
            this.statusCode = statusCode;
            this.data = data;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public ApiResponseDto getData() {
            return data;
        }
    }
}