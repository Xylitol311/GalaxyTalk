package com.galaxytalk.gateway.config;

import com.galaxytalk.gateway.dto.ApiResponseDto;
import com.galaxytalk.gateway.filter.CustomAccessDeniedHandler;
import com.galaxytalk.gateway.filter.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) //jwt 토큰으로 인증하는 방식이라 csrf disable 상관 없음
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) //jwt 토큰으로 인증하는 방식이라 보안 컨텐스트 저장 안해도됨
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll() //OPTIONS : 브라우저가 요청할 메서드와 헤더를 허용하는지 미리 확인하는용 보안과 관련 없음
                        .pathMatchers("/login/**", "/oauth2/authorization/**","/api/oauth/test").permitAll()
                        .pathMatchers("/api/oauth/signup").hasRole("GUEST")
                        .pathMatchers("/api/oauth/**", "api/match/**","api/support/**").hasRole("USER")
                        .anyExchange().authenticated())
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec.accessDeniedHandler(new CustomAccessDeniedHandler())) //권한이 없을때에 대한 예외 처리
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)  // JWT 필터 통과
                .build();
    }

}