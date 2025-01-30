package com.galaxytalk.auth.controller;

import com.galaxytalk.auth.entity.Users;
import com.galaxytalk.auth.jwt.JWTUtil;
import com.galaxytalk.auth.service.RefreshTokenService;
import com.galaxytalk.auth.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth")
public class AuthController {


    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(JWTUtil jwtUtil, UserService userService, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    /*
    추가 구현 필요
    1. 회원가입 시 로직 구현
    2. JWT 토큰 담아서 처리해야하는거 구현
    3. refresh token + redis
    4. restcontroller로 들어올때 로그인 페이지로 넘어가게 하는거 구현
     */

    //localhost:8080/oauth2/authorization/naver 로 리다이렉트 시키면 됨

    @PostMapping("/refresh")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {

        //get refresh token
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {
            if(cookie.getName().equals("RefreshToken")){
                refreshToken = cookie.getValue();
                break;
            }
        }


        if (refreshToken == null) {

            //response status code
            return new ResponseEntity<>("refresh token null", HttpStatus.BAD_REQUEST);
        }

        //expired check
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {

            //response status code
            return new ResponseEntity<>("refresh token expired", HttpStatus.BAD_REQUEST);
        }

            //1) 유저 정보 가져오기
            String serialNumber = jwtUtil.getUserId(refreshToken);
            String role = jwtUtil.getRole(refreshToken);


            //2) 토큰 생성
            String accessToken = jwtUtil.token(serialNumber,role , 1000*60*60*1); //1시간
            String newRefreshToken = jwtUtil.token(serialNumber,role , 1000*60*60*24*3); //3일


            //3) 만들어진 토큰은 클라이언트에 쿠키에 담아서 주기
            response.addCookie(createCookie("AccessToken", accessToken));
            response.addCookie(createCookie("RefreshToken", newRefreshToken));

            //4) 리프레시 토큰 기존거는 삭제하고 새로운것은 레디스에 넣기
            refreshTokenService.removeRefreshToken(refreshToken);
            refreshTokenService.saveTokenInfo(newRefreshToken, accessToken);

        return new ResponseEntity<>(HttpStatus.OK);
    }

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


