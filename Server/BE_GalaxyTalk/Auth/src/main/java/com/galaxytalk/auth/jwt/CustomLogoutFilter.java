package com.galaxytalk.auth.jwt;

import com.galaxytalk.auth.repository.RefreshTokenRepository;
import com.galaxytalk.auth.service.RefreshTokenService;
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

    public CustomLogoutFilter(JWTUtil jwtUtil, RefreshTokenService refreshTokenService) {

        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
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

        // 토큰 검증
        if (refresh == null || access == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!validateToken(refresh, response, HttpServletResponse.SC_BAD_REQUEST)) return;
        if (!validateToken(access, response, HttpServletResponse.SC_UNAUTHORIZED)) return;

        // 로그아웃 진행 - Refresh 토큰 DB에서 제거
        refreshTokenService.removeRefreshToken(refresh);

        // 토큰 쿠키 삭제
        deleteCookie(response, "RefreshToken");
        deleteCookie(response, "AccessToken");

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private boolean validateToken(String token, HttpServletResponse response, int errorCode) throws IOException {
        try {
            jwtUtil.isExpired(token);
            return true;
        } catch (ExpiredJwtException e) {

            response.setStatus(errorCode);
            return false;
        }
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // 기존 쿠키 속성과 동일하게 유지
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

}
