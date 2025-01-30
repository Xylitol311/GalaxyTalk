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

        //get refresh token
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("RefreshToken")) {
                refresh = cookie.getValue();
            }
        }

        //refresh null check
        if (refresh == null) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        //expired check
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            // Log the error for debugging
            System.out.println("Expired JWT exception: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        //로그아웃 진행
        //Refresh 토큰 DB에서 제거
        refreshTokenService.removeRefreshToken(refresh);

        //토큰 Cookie에서 삭제
        Cookie refreshCookie = new Cookie("RefreshToken", null);
        Cookie accessCookie = new Cookie("AccessToken", null);
        refreshCookie.setMaxAge(0);
        accessCookie.setMaxAge(0);
        refreshCookie.setPath("/");
        accessCookie.setPath("/");


        response.addCookie(refreshCookie);
        response.addCookie(accessCookie);

        response.setStatus(HttpServletResponse.SC_OK);
    }
}
