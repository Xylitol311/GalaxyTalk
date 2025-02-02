package com.galaxytalk.auth.controller;

import com.galaxytalk.auth.dto.UserSend;
import com.galaxytalk.auth.entity.Planets;
import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.entity.Users;
import com.galaxytalk.auth.jwt.JWTUtil;
import com.galaxytalk.auth.service.PlanetService;
import com.galaxytalk.auth.service.RefreshTokenService;
import com.galaxytalk.auth.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/oauth")
public class AuthController {


    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final PlanetService planetService;

    public AuthController(JWTUtil jwtUtil, UserService userService, RefreshTokenService refreshTokenService, PlanetService planetService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.planetService = planetService;
    }

    //수정 필요 내용
    /*
    1. 쿠키까서 user ID 가져오는거 게이트 웨이로 바꾸기
    2. 중복되는 부분 메서드로 처리하기
     */


    //로그인 : localhost:8080/oauth2/authorization/naver 로 리다이렉트 시키면 됨

    //회원가입
    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> signUp(HttpServletRequest request, HttpServletResponse response,
                                    @RequestParam("mbti") String mbti, @RequestParam("planetId") int planetId) {


        // 1. 쿠키에서 AccessToken 추출
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ResponseEntity<>("쿠키가 비어있습니다.", HttpStatus.BAD_REQUEST);
        }

        String accessToken = Arrays.stream(cookies)
                .filter(cookie -> "AccessToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        String refreshToken = Arrays.stream(cookies)
                .filter(cookie -> "RefreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        if (accessToken.isEmpty() || refreshToken.isEmpty()) {
            return new ResponseEntity<>("토큰이 확인되지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        // 2. AccessToken에서 시리얼 번호 추출
        String serialNumber = jwtUtil.getUserId(accessToken);

        // 3. 시리얼 번호로 사용자 정보 조회
        Users user = userService.getUserBySerialNumber(serialNumber);
        if (user == null) {
            return new ResponseEntity<>("유저가 확인되지 않습니다", HttpStatus.BAD_REQUEST);
        }

        // 4. 사용자 정보 수정
        user.setMbti(mbti);
        user.setRole(Role.ROLE_USER);

        // 5. 행성 정보 조회 및 설정
        Planets planet = planetService.getPlanetById(planetId);
        if (planet == null) {
            return new ResponseEntity<>("확인되지 않은 행성입니다.", HttpStatus.BAD_REQUEST);
        }
        user.setPlanets(planet);

        // 6. 토큰 수정하기

        //6-1) 토큰 생성
        String newAccessToken = jwtUtil.token(serialNumber, user.getRole().toString(), 1000*60*60*1); //1시간
        String newRefreshToken = jwtUtil.token(serialNumber,user.getRole().toString() , 1000*60*60*24*3); //3일


        //6-2) 만들어진 토큰은 클라이언트에 쿠키에 담아서 주기
        response.addCookie(createCookie("AccessToken", newAccessToken));
        response.addCookie(createCookie("RefreshToken", newRefreshToken));

        //6-3) 리프레시 토큰 기존거는 삭제하고 새로운것은 레디스에 넣기
        refreshTokenService.removeRefreshToken(refreshToken);
        refreshTokenService.saveTokenInfo(newRefreshToken);


        return new ResponseEntity<>(HttpStatus.OK);
    }


    //회원정보 조회
    @GetMapping
    public ResponseEntity<?> getUserInfo(HttpServletRequest request) {


        // 1. 쿠키에서 AccessToken 추출
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ResponseEntity<>("쿠키가 비어있습니다.", HttpStatus.BAD_REQUEST);
        }

        String accessToken = Arrays.stream(cookies)
                .filter(cookie -> "AccessToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        if (accessToken == null) {
            return new ResponseEntity<>("토큰이 확인되지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        // 2. AccessToken에서 시리얼 번호 추출
        String serialNumber = jwtUtil.getUserId(accessToken);

        // 3. 시리얼 번호로 사용자 정보 조회
        Users user = userService.getUserBySerialNumber(serialNumber);


        if (user == null) {
            return new ResponseEntity<>("유저가 확인되지 않습니다", HttpStatus.BAD_REQUEST);
        }

        UserSend getUser = new UserSend(user);

        return ResponseEntity.status(HttpStatus.OK).body(getUser);
    }


    //회원정보 수정
    @PutMapping("/update")
    @Transactional
    public ResponseEntity<?> update(HttpServletRequest request,
                                    @RequestParam("mbti") String mbti, @RequestParam("planetId") int planetId) {


        // 1. 쿠키에서 AccessToken 추출
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ResponseEntity<>("쿠키가 비어있습니다.", HttpStatus.BAD_REQUEST);
        }

        String accessToken = Arrays.stream(cookies)
                .filter(cookie -> "AccessToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);


        if (accessToken.isEmpty()) {
            return new ResponseEntity<>("토큰이 확인되지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        // 2. AccessToken에서 시리얼 번호 추출
        String serialNumber = jwtUtil.getUserId(accessToken);

        // 3. 시리얼 번호로 사용자 정보 조회
        Users user = userService.getUserBySerialNumber(serialNumber);
        if (user == null) {
            return new ResponseEntity<>("유저가 확인되지 않습니다", HttpStatus.BAD_REQUEST);
        }

        // 4. 사용자 정보 수정
        user.setMbti(mbti);

        // 5. 행성 정보 조회 및 설정
        Planets planet = planetService.getPlanetById(planetId);
        if (planet == null) {
            return new ResponseEntity<>("확인되지 않은 행성입니다.", HttpStatus.BAD_REQUEST);
        }
        user.setPlanets(planet);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    //회원탈퇴 (SOFT DELETE)
    @PutMapping("/withdraw")
    @Transactional
    public ResponseEntity<?> withdraw(HttpServletRequest request, HttpServletResponse response) {

        // 1. 쿠키에서 AccessToken 추출
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ResponseEntity<>("쿠키가 비어있습니다.", HttpStatus.BAD_REQUEST);
        }

        String accessToken = Arrays.stream(cookies)
                .filter(cookie -> "AccessToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        String refreshToken = Arrays.stream(cookies)
                .filter(cookie -> "RefreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        if (accessToken.isEmpty() || refreshToken.isEmpty()) {
            return new ResponseEntity<>("토큰이 확인되지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        // 2. AccessToken에서 시리얼 번호 추출
        String serialNumber = jwtUtil.getUserId(accessToken);

        // 3. 시리얼 번호로 사용자 정보 조회
        Users user = userService.getUserBySerialNumber(serialNumber);
        if (user == null) {
            return new ResponseEntity<>("유저가 확인되지 않습니다", HttpStatus.BAD_REQUEST);
        }

        // 4. 사용자 withdrawn 정보 수정
        user.setWithdrawnAt(LocalDateTime.now());

        // 5. 사용자 role 수정
        user.setRole(Role.ROLE_WITHDRAW);


        // 6. 쿠키 전부 삭제
        clearCookies(response);

        //7. 레디스에서 rf 삭제
        refreshTokenService.removeRefreshToken(refreshToken);


        return new ResponseEntity<>(HttpStatus.OK);
    }

    //엑세스 토큰, 리프레스 토큰 갱신
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
            refreshTokenService.saveTokenInfo(newRefreshToken);

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
    private void clearCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("AccessToken", null);
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("RefreshToken", null);
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    }

}


