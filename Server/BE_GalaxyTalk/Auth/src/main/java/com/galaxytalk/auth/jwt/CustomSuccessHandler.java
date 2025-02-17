package com.galaxytalk.auth.jwt;

import com.galaxytalk.auth.dto.CustomOAuth2User;
import com.galaxytalk.auth.service.RefreshTokenService;
import com.galaxytalk.auth.service.UserStatusService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            log.info("onAuthenticationSuccess 메서드에 들어옴");
            // 성공 시 받은 사용자 정보 처리
            CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

            log.info("customUserDetails에 값 들어옴");

            // provider로부터 받은 serialNumber
            String serialNumber = customUserDetails.getName();
            log.info("네이버/카카오로부터 받은 serialNumber: {}", serialNumber);

            // 권한 정보 읽기
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            String role = iterator.next().getAuthority();
            log.info("권한 읽어옴: {}", role);

            // 토큰 생성
            String accessToken = jwtUtil.token(serialNumber, role, 1000 * 60 * 1); // 1분임 ->  * 60(1시간) * 24(하루)
            String refreshToken = jwtUtil.token(serialNumber, role, 1000 * 60 * 60 * 24 * 3); // 3일
            log.info("토큰 생성 완료, accessToken: {}", accessToken);

            // 생성된 토큰을 쿠키에 담아서 전달
            response.addCookie(createCookie("AccessToken", accessToken));
            response.setStatus(HttpStatus.OK.value());
            log.info("쿠키에 토큰 담아 전달");

            // 리프레시 토큰 저장 및 사용자 상태 업데이트
            refreshTokenService.saveTokenInfo(accessToken, refreshToken);
            userStatusService.saveUserStatus(serialNumber, "idle");
            log.info("토큰과 유저 상태 저장 완료");

            // 권한에 따른 리다이렉트 처리
            if ("ROLE_GUEST".equals(role)) {
                response.sendRedirect(frontUrl + "signup");
            } else if ("ROLE_USER".equals(role)) {
                response.sendRedirect(frontUrl);
            } else {
                response.sendRedirect(frontUrl + "signup");
            }
        } catch (Exception e) {
            log.error("토큰 생성 및 저장 과정 중 오류 발생: ", e);
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "토큰 처리 중 오류 발생");
        }
    }

    //쿠기 만드는 메서드
    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value) ;

        //쿠키의 유효기간 설정
        cookie.setMaxAge(60); //1시간간 //*60해야지 1시간
        //SSL 통신채널 연결 시에만 쿠키를 전송하도록 설정
        cookie.setSecure(true);

        //브라우저가 쿠키값을 전송할 URL 지정
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "None");
//        cookie.setDomain("i12a503.p.ssafy.io");

        //브라우저에서(javascript를 통해) 쿠키에 접근할 수 없도록 제한
        cookie.setHttpOnly(true);

        return cookie;
    }

}
