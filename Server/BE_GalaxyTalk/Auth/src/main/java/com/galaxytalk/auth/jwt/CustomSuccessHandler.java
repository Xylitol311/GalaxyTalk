package com.galaxytalk.auth.jwt;

import com.galaxytalk.auth.dto.CustomOAuth2User;

import com.galaxytalk.auth.entity.RefreshToken;
import com.galaxytalk.auth.service.RefreshTokenService;
import com.galaxytalk.auth.service.UserStatusService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${front.url}")
    private String frontUrl;


    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserStatusService userStatusService;

    public CustomSuccessHandler(JWTUtil jwtUtil, RefreshTokenService refreshTokenService, UserStatusService userStatusService) {

        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userStatusService = userStatusService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException, IOException {

        //성공할 경우 받은 데이터 처리
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

        //데이터 1) provider(네이버)로 부터 받은 serialNumber
        String serialNumber = customUserDetails.getName();

        //데이터 2) 권한 읽어오기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        //토큰 생성
        String accessToken = jwtUtil.token(serialNumber, role, 1000*60*60*1); //1시간
        String refreshToken = jwtUtil.token(serialNumber, role, 1000 * 60 * 60 * 24 * 3); //3일


        //만들어진 토큰은 클라이언트데 쿠키에 담아서 주기
        response.addCookie(createCookie("AccessToken", accessToken));

        response.setStatus(HttpStatus.OK.value());

        //리프레시 토큰 레디스에 넣기, 유저 상태 관리 시작
        refreshTokenService.saveTokenInfo(accessToken,refreshToken);
        userStatusService.saveUserStatus(serialNumber, "idle");


        // 권한에 따른 로그인 후 로직 분기
        if(role.equals("ROLE_GUEST")) {
            response.sendRedirect(frontUrl+"/signup");
        }else if(role.equals("ROLE_USER")){
            response.sendRedirect(frontUrl);
        }else{
            response.sendRedirect(frontUrl+"/signup");
        }
    }

    //쿠기 만드는 메서드
    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value) ;

        //쿠키의 유효기간 설정
        cookie.setMaxAge(60*60); //1시간간
        //SSL 통신채널 연결 시에만 쿠키를 전송하도록 설정
        cookie.setSecure(true);

        //브라우저가 쿠키값을 전송할 URL 지정
        cookie.setPath("/");

        //브라우저에서(javascript를 통해) 쿠키에 접근할 수 없도록 제한
        cookie.setHttpOnly(true);

        return cookie;
    }

}
