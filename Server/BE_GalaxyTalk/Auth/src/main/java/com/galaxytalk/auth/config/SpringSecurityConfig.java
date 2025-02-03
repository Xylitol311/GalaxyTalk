package com.galaxytalk.auth.config;

import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.jwt.CustomLogoutFilter;
import com.galaxytalk.auth.jwt.CustomSuccessHandler;
import com.galaxytalk.auth.jwt.JWTFilter;
import com.galaxytalk.auth.jwt.JWTUtil;
import com.galaxytalk.auth.service.CustomOAuth2UserService;
import com.galaxytalk.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

@Configuration
//스프링 시큐리티의 필터 체인이 동작하여 요청을 인가하고 인증
@EnableWebSecurity
public class SpringSecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public SpringSecurityConfig(CustomOAuth2UserService customOAuth2UserService, CustomSuccessHandler customSuccessHandler, JWTUtil jwtUtil, RefreshTokenService refreshTokenService) {

        this.customOAuth2UserService = customOAuth2UserService;
        this.customSuccessHandler = customSuccessHandler;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    // 스프링세큐리티가 작동 시 전반적인 필터체인 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {


        http
                .csrf((csrf) -> csrf.disable()); //csrf(html tag통한 공격) 보안 비활성

        http
                .formLogin((login) -> login.disable()); //springsecurity에서 지원하는 기본폼 비활성

        http
                .httpBasic((basic) -> basic.disable()); //http basic auth 기본 인증 로그인 비활성
        http
                .addFilterAfter(new JWTFilter(jwtUtil), OAuth2LoginAuthenticationFilter.class); //JWTFilter 추가
        http
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshTokenService), LogoutFilter.class);
        http
                .oauth2Login((oauth2) -> oauth2 //oauth2Login -> 필터 자동 설정, client는 커스텀 필요
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig //OAuth 2.0 인증 후, 사용자 정보를 어떻게 가져올지 설정하는 부분
                                .userService(customOAuth2UserService)) // 사용자 정보를 처리하는 커스텀 서비스를 설정
                        .successHandler(customSuccessHandler)
                );


        http
                //특정 url들에 대한 접근 방법을 설정할 수 있다. 동작되는 순서는 상단부터 적용되므로, 순서를 주의
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers( "/login/**","/api/oauth/signup").permitAll()
                        .requestMatchers( "/api/oauth/**").hasRole("USER")
                        .anyRequest().authenticated()); // permitAll -> 허용, 그외 인증 필요

        //세션 설정 : STATELESS
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//        //cors 에러 처리
//        http
//                .cors((corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
//
//                    @Override
//                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
//
//                        CorsConfiguration configuration = new CorsConfiguration();
//
//                        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
//                        configuration.setAllowedMethods(Collections.singletonList("*"));
//                        configuration.setAllowCredentials(true);
//                        configuration.setAllowedHeaders(Collections.singletonList("*"));
//                        configuration.setMaxAge(3600L);
//
//                        configuration.setExposedHeaders(Collections.singletonList("Set-Cookie"));
//                        configuration.setExposedHeaders(Collections.singletonList("AccessToken"));
//
//                        return configuration;
//                    }
//                })));
        return http.build();
    }
}