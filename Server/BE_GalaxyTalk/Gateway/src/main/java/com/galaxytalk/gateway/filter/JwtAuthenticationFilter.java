package com.galaxytalk.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galaxytalk.gateway.dto.ApiResponseDto;
import com.galaxytalk.gateway.jwt.JWTUtil;


import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JWTUtil jwtUtil;

    public JwtAuthenticationFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        System.out.println("Request Method: " + request.getMethod());
        System.out.println("Request Headers: " + request.getHeaders());
        System.out.println("Request Cookies: " + request.getCookies());

        // OPTIONS 요청은 바로 필터 체인을 실행하고 종료

        String path = exchange.getRequest().getURI().getPath();

        System.out.println("들어오고 있는 경로 : " + path);

        // ✅ 특정 경로 제외
        if (path.startsWith("/oauth2/authorization/") || request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // ✅ AccessToken 쿠키 추출
        String token = request.getCookies().getFirst("AccessToken") != null ?
                request.getCookies().getFirst("AccessToken").getValue() : null;

        if (token == null || isTokenExpired(token)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        // 2. 토큰에서 userId와 role 추출
        String userId = jwtUtil.getSerialNumber(token);
        String role = jwtUtil.getRole(token);

        // 3. 새로운 요청 객체 생성 (헤더에 userId 추가)
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove("X-Real-IP");
                    httpHeaders.remove("X-Forwarded-For");
                    httpHeaders.remove("X-Forwarded-Proto");
                    httpHeaders.remove("X-Forwarded-Port");
                    httpHeaders.remove("X-Forwarded-Host");
                    httpHeaders.remove("Forwarded");
                })
                .header("X-User-ID", userId)  // 헤더 추가는 마지막에
                .build();


        System.out.println("User ID: " + userId);
        System.out.println("Role: " + role);

        // 4. Security Context에 Authentication 저장
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, null, AuthorityUtils.createAuthorityList(role)
        );
        SecurityContext securityContext = new SecurityContextImpl(authentication);

        if(CorsUtils.isPreFlightRequest(request)){
            return chain.filter(exchange);
        }

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
        // 5. Spring Security가 감지할 수 있도록 SecurityContext 설정


    }
    private boolean isTokenExpired(String token) {
        try {
            return jwtUtil.isExpired(token);
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        // ApiResponseDto를 JSON 문자열로 변환
        try {
            String errorResponse = new ObjectMapper().writeValueAsString(ApiResponseDto.noAccessTokenResponse());

            // 에러 응답을 작성
            DataBuffer buffer = response.bufferFactory().wrap(errorResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // 예외 처리: JSON 변환 오류 발생 시
            e.printStackTrace();
            return response.setComplete();
        }
    }

}

