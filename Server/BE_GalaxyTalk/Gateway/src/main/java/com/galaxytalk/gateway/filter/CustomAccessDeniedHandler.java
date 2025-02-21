package com.galaxytalk.gateway.filter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galaxytalk.gateway.dto.ApiResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
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


    //mono는 리액티브 프로그래밍(비동기) 핵심 컴포넌트, 단일 응답처리 할때 많이 쓰임
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN); //권한없음으로 설정
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON); //json으로 응답 하기 위해, API 응답의 표준적인 관행



        // ApiResponseDto를 사용하여 JSON 응답 생성
        ApiResponseDto response = ApiResponseDto.forbiddenResponse();

        try {
            // JSON 변환 후 응답을 Buffer에 저장
            String jsonResponse = objectMapper.writeValueAsString(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));

            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}