package com.galaxytalk.auth.jwt;

import com.galaxytalk.auth.dto.CustomOAuth2User;

import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

// 로그인 성공시 부가 작업
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public CustomSuccessHandler(JWTUtil jwtUtil, RefreshTokenService refreshTokenService) {

        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException, IOException {

        System.out.println("데이터 처리하고 토큰 만들기....");


        //성공할 경우 받은 데이터 처리
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

        //데이터 1) provider(네이버)로 부터 받은 id -> id가 아닌 다름 값을 토큰에 넣을건지 논의 필요
        String id = customUserDetails.getName();

        //데이터 2) 권한 읽어오기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        //토큰 생성
        String accessToken = jwtUtil.token(id, role, 1000*60*60*1); //1시간
        String refreshToken = jwtUtil.token(id, role, 1000 * 60 * 60 * 24 * 3); //3일

        //만들어진 토큰은 클라이언트데 쿠키에 담아서 주기
        response.addCookie(createCookie("AccessToken", accessToken));
        response.addCookie(createCookie("RefreshToken",refreshToken));
        response.setStatus(HttpStatus.OK.value());

        //리프레시 토큰 레디스에 넣기
        refreshTokenService.saveTokenInfo(refreshToken);


        if(role.equals("ROLE_GUEST")) {
            response.sendRedirect("http://localhost:3000/signup");
        }else{
            response.sendRedirect("http://localhost:3000/");
            System.out.println("회원가입이 아닌 홈으로 옴");
        }
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
