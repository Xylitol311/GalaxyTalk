package com.galaxytalk.auth.jwt;

import com.galaxytalk.auth.dto.CustomOAuth2User;
import com.galaxytalk.auth.dto.UserDTO;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryBasedSynchronizationStrategy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//어떤 요청이 들어올떄마다 JWT 토큰있는지 확인
//토큰에 이슈가 없을 경우 SecurityContextHolder에 담음 -> 여기에 담은 데이터로 체크
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {

        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //cookie들을 불러온 뒤 Authorization Key에 담긴 쿠키를 찾음
        String accessToken = null;


        Cookie[] cookies = request.getCookies();
        if (cookies != null) {  // 이 체크 추가 필요
            for (Cookie cookie : cookies) {

                System.out.println(cookie.getName());
                if (cookie.getName().equals("AccessToken")) {

                    accessToken = cookie.getValue();

                }
            }
        }

        //쿠키에 accessToken이 비어 있을 경우 메서드 종료
        if (accessToken == null) {

            System.out.println("accessToken null");
            filterChain.doFilter(request, response);

            //조건이 해당되면 메소드 종료 (필수)
            return;
        }


        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {

            //response status code
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }


        //토큰에서 username과 role 획득
        String username = jwtUtil.getUserId(accessToken);
        String role = jwtUtil.getRole(accessToken);


        //userDTO를 생성하여 값 set
        UserDTO userDTO = new UserDTO();
        userDTO.setName(username);
        userDTO.setRole(role);

          //UserDetails에 회원 정보 객체 담기
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDTO);

        System.out.println("너의 ROLE은 무엇이냐!!!");
        System.out.println(role);


        //스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());
        //세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    //쿠기 만드는 메서드
    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value) ;

        //쿠키의 유효기간 설정
        cookie.setMaxAge(60*60); //1시간간
        //SSL 통신채널 연결 시에만 쿠키를 전송하도록 설정
        //cookie.setSecure(true);

        //브라우저가 쿠키값을 전송할 URL 지정
        cookie.setPath("/");

        //브라우저에서(javascript를 통해) 쿠키에 접근할 수 없도록 제한
        cookie.setHttpOnly(true);

        return cookie;
    }


}
