package com.galaxytalk.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galaxytalk.auth.dto.ApiResponseDto;
import com.galaxytalk.auth.service.RefreshTokenService;
import com.galaxytalk.auth.service.UserStatusService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//로그아웃시 작동되는 필터
//클라이언트에서 http://localhost:8080/api/oauth/logout POST로 요청
public class CustomLogoutFilter extends GenericFilterBean {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserStatusService userStatusService;

    public CustomLogoutFilter(JWTUtil jwtUtil, RefreshTokenService refreshTokenService, UserStatusService userStatusService) {

        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userStatusService = userStatusService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        //path and method verify
        String requestUri = request.getRequestURI();
        if (!requestUri.matches("^\\/api/oauth/logout$")) {

            filterChain.doFilter(request, response);
            return;
        }


        String requestMethod = request.getMethod();
        if (!requestMethod.equals("POST")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키 가져오기
        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }

        String refresh = cookies.get("RefreshToken");
        String access = cookies.get("AccessToken");


        // 토큰 검증 (이미 GATEWAY에서 함)


        // 로그아웃 진행 - Refresh 토큰 DB에서 제거
        refreshTokenService.removeRefreshToken(refresh);
        userStatusService.removeUserStatus(jwtUtil.getSerialNumber(access));

        // 토큰 쿠키 삭제
        deleteCookie(response, "RefreshToken");
        deleteCookie(response, "AccessToken");

        ApiResponseDto apiResponseDto = new ApiResponseDto("로그아웃에 성공했습니다.",null);

        sendResponse(response, apiResponseDto);
    }


    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // 기존 쿠키 속성과 동일하게 유지
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    private void sendResponse(HttpServletResponse response, ApiResponseDto apiResponseDto) throws IOException {
        // 응답 상태 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponseDto));
    }

}
