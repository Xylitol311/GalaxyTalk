package com.galaxytalk.auth.config;

import com.galaxytalk.auth.jwt.CustomLogoutFilter;
import com.galaxytalk.auth.jwt.CustomSuccessHandler;
import com.galaxytalk.auth.jwt.JWTUtil;
import com.galaxytalk.auth.service.CustomOAuth2UserService;
import com.galaxytalk.auth.service.RefreshTokenService;
import com.galaxytalk.auth.service.UserStatusService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;



@Configuration
//스프링 시큐리티의 필터 체인이 동작하여 요청을 인가하고 인증
@EnableWebSecurity
public class SpringSecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserStatusService userStatusService;

    public SpringSecurityConfig(CustomOAuth2UserService customOAuth2UserService, CustomSuccessHandler customSuccessHandler, JWTUtil jwtUtil, RefreshTokenService refreshTokenService, UserStatusService userStatusService) {

        this.customOAuth2UserService = customOAuth2UserService;
        this.customSuccessHandler = customSuccessHandler;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userStatusService = userStatusService;
    }

    // 스프링세큐리티가 작동 시 전반적인 필터체인 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshTokenService, userStatusService), LogoutFilter.class) //로그아웃 시 필터
                .oauth2Login(oauth2 -> oauth2 //로그인시 필터
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                                .userService(customOAuth2UserService)) //로그인 필터
                        .successHandler(customSuccessHandler) //로그인 성공한 후 필터
                )
                //모든 요청에 대해 permitAll(이미 게이트웨이에서 체크 해주기 때문에)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()
                        .anyRequest().authenticated())

                //JWT 토큰으로 stateless 상태 유지
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }


}