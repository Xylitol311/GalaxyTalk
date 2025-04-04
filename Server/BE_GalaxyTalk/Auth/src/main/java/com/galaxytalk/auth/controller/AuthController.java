package com.galaxytalk.auth.controller;

import com.galaxytalk.auth.dto.ApiResponseDto;
import com.galaxytalk.auth.dto.EnergyRequest;
import com.galaxytalk.auth.dto.UserSendDTO;
import com.galaxytalk.auth.dto.UserSignup;
import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.entity.Users;
import com.galaxytalk.auth.jwt.JWTUtil;
import com.galaxytalk.auth.service.PlanetService;
import com.galaxytalk.auth.service.RefreshTokenService;
import com.galaxytalk.auth.service.UserService;
import com.galaxytalk.auth.service.UserStatusService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.Map;


//oauth(로그인,회원가입) 및 회원 CRUD 컨트롤러
@RestController
@RequestMapping("/api/oauth")
public class AuthController {

    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final PlanetService planetService;
    private final UserStatusService userStatusService;


    public AuthController(JWTUtil jwtUtil, UserService userService, RefreshTokenService refreshTokenService, PlanetService planetService, UserStatusService userStatusService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.planetService = planetService;
        this.userStatusService = userStatusService;
    }

    //# 로그인 : localhost:8080/oauth2/authorization/naver 로 리다이렉트 시키면 됨
    //# 회원가입
    @PostMapping("/signup")
    //# serialNumber -> 게이트웨이에서 받아서 user 가져오고 수정시 사용, HTTP req -> 쿠키까기용(리프레시 갱신 - role 변경), HTTP res -> 쿠키 담아주기 용
    public ResponseEntity<?> signUp(@RequestHeader("X-User-ID") String serialNumber, HttpServletRequest request, HttpServletResponse response,
                                    @RequestBody UserSignup userSignup) {

        // 1. 쿠키 받아오기 & 없을 경우 에러처리( 리프레시 토큰 가져오기 용 )
        Cookie[] cookies = request.getCookies();

        String accessToken = Arrays.stream(cookies)
                .filter(cookie -> "AccessToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
        if (accessToken.isEmpty()) {
            return new ResponseEntity<>(ApiResponseDto.noAccessToken, HttpStatus.UNAUTHORIZED);
        }

        // 1-1) 기존 리프레시 토큰 검증 및 삭제
        if(!refreshTokenService.removeRefreshToken(accessToken))
            return ResponseEntity.status(499).body(ApiResponseDto.noRefreshToken);

        //2. user DB에 저장
        if(!userService.signup(serialNumber,userSignup.getMbti(),userSignup.getPlanetId()))
            return new ResponseEntity<>(ApiResponseDto.
                    badRequestUser, HttpStatus.BAD_REQUEST);


        // 3. 토큰 수정하기
        //3-1) 다시 넣어줄 토큰 생성(ROLE이 변경되었기 때문에)
        String newAccessToken = jwtUtil.token(serialNumber, Role.ROLE_USER.toString(), 1000 * 60 * 60 *24 *7); //7일
        String newRefreshToken = jwtUtil.token(serialNumber, Role.ROLE_USER.toString(), 1000 * 60 * 60 * 24 * 3); //3일

        //3-2) 만들어진 토큰은 클라이언트에 쿠키에 담아서 주기 & 리프레스 토큰 레디스에 추가
        response.addCookie(createCookie("AccessToken", newAccessToken));
        refreshTokenService.saveTokenInfo(newAccessToken,newRefreshToken);

        ApiResponseDto goodResponse = new ApiResponseDto("회원가입이 완료 되었습니다.", null);
        return new ResponseEntity<>(goodResponse, HttpStatus.OK);
    }

    //# 회원정보 조회
    @GetMapping
    //# serialNumber -> 게이트웨이에서 받아서 user 가져오고 수정시 사용
    public ResponseEntity<?> getUserInfo(@RequestHeader("X-User-ID") String serialNumber) {

        //1. serialNumber로 user 정보 가져오기 & 예외처리
        Users user = userService.getUserBySerialNumber(serialNumber);


        if (user == null) {
            return new ResponseEntity<>(ApiResponseDto.badRequestUser, HttpStatus.BAD_REQUEST);
        }

        //2. 보내줄 userSend 객체 생성
        UserSendDTO getUser = new UserSendDTO(user.getSerialNumber(), user.getMbti(), user.getEnergy(), user.getRole(), user.getPlanets().getId());

        ApiResponseDto okData = new ApiResponseDto(getUser);
        return new ResponseEntity<>(okData, HttpStatus.OK);
    }

    //# 회원정보 수정
    @PutMapping("/update")
    //# serialNumber -> 게이트웨이에서 받아서 user 가져오고 수정시 사용
    public ResponseEntity<?> update(@RequestHeader("X-User-ID") String serialNumber,
                                    @RequestBody UserSignup userSignup) {

        //1. 유저 정보 조회,수정 및 예외처리
        if(!userService.signup(serialNumber,userSignup.getMbti(),userSignup.getPlanetId()))
            return new ResponseEntity<>(ApiResponseDto.
                    badRequestUser, HttpStatus.BAD_REQUEST);

        ApiResponseDto goodResponse = new ApiResponseDto("회원가입이 수정이 완료 되었습니다.", null);
        return new ResponseEntity<>(goodResponse, HttpStatus.OK);
    }


    //# 회원탈퇴 (SOFT DELETE)
    @PutMapping("/withdraw")
    //# serialNumber -> 게이트웨이에서 받아서 user 가져오고 수정시 사용, HTTP req -> 쿠키까기용(리프레시 가져오는 용) , HTTP res -> 삭제용 쿠키 담아주기 용
    public ResponseEntity<?> withdraw(@RequestHeader("X-User-ID") String serialNumber, HttpServletRequest request, HttpServletResponse response) {

        // 1. 쿠키 조회 및 삭제
        //1-1) 토큰 조회 및 예외처리
        Cookie[] cookies = request.getCookies();

        String accessToken = Arrays.stream(cookies)
                .filter(cookie -> "AccessToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        //accessToken이 비어있는거는 재로그인 필요
        if (accessToken.isEmpty())
            return ResponseEntity.status(499).body(ApiResponseDto.noRefreshToken);

        //2) 토큰들 다 삭제처리
        if(!refreshTokenService.removeRefreshToken(accessToken))
            return ResponseEntity.status(499).body(ApiResponseDto.noRefreshToken);

        Cookie accessTokenCookie = new Cookie("AccessToken", null);
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true); // 기존 쿠키 속성과 동일하게 유지
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setAttribute("SameSite","None");
        response.addCookie(accessTokenCookie);

        //3.유저 정보 조회 및 DB SOFTDELETE
        if(!userService.withDraw(serialNumber))
            return new ResponseEntity<>(ApiResponseDto.
                    badRequestUser, HttpStatus.BAD_REQUEST);


        ApiResponseDto goodResponse = new ApiResponseDto("회원탈퇴가 완료 되었습니다.", null);
        return new ResponseEntity<>(goodResponse, HttpStatus.OK);
    }

    //# 엑세스 토큰, 리프레스 토큰 갱신
    @PostMapping("/refresh")
    //# serialNumber -> 게이트웨이에서 받아서 user 가져오고 수정시 사용, HTTP req -> 쿠키까기용, HTTP res -> 새로운 쿠키 담아주기 용
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {

        //1. 쿠키에서 엑세스 토큰 까기
        String accessToken = null;
        Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("AccessToken")) {
                accessToken = cookie.getValue();
                break;
            }
        }

        //1-1) 토큰 비어있을 경우 재로그인 필요 ( 이부분은 permitall & 토큰 검증 없어서 따로 해줘야함 )
        if (accessToken == null) {
            //response status code

            return ResponseEntity.status(499).body(ApiResponseDto.noRefreshToken);
        }

        //2. refreshToken 가져오기
        String refreshToken = refreshTokenService.getRefreshToken(accessToken);

        //3. refreshToken 검증
        if(refreshToken==null){
            return ResponseEntity.status(499).body(ApiResponseDto.noRefreshToken);
        }

        if(jwtUtil.isExpired(refreshToken)){
            return ResponseEntity.status(499).body(ApiResponseDto.noRefreshToken);
        }

        //4. 새로운 토큰 생성
        String newAccessToken = jwtUtil.token(jwtUtil.getSerialNumber(refreshToken), jwtUtil.getRole(refreshToken), 1000 * 60 * 1); //1분 60 *24 => 하루
        String newRefreshToken = jwtUtil.token(jwtUtil.getSerialNumber(refreshToken), jwtUtil.getRole(refreshToken), 1000 * 60 * 60 * 24*3); //3일



        //5. 레디스 삭제(레디스에 이상 여부 함께 확인) 하고 새로운 값은 넣기
        if (!refreshTokenService.removeRefreshToken(accessToken)) {
            return ResponseEntity.status(499).body(ApiResponseDto.noRefreshToken);
        }
        refreshTokenService.saveTokenInfo(newAccessToken, newRefreshToken);


        //6. 엑세스토큰 쿠키에 담기
        response.addCookie(createCookie("AccessToken", newAccessToken));

        ApiResponseDto goodResponse = new ApiResponseDto("리프레시 토큰 갱신이 완료 되었습니다.", null);
        return new ResponseEntity<>(goodResponse, HttpStatus.OK);
    }


    //# 회원 상태 조회
    @GetMapping("status")
    public ResponseEntity<?> getUserStatus(@RequestHeader("X-User-ID") String serialNumber) {

        //1. 유저 상태 조회
        Map<String, String> status = userStatusService.getUserStatus(serialNumber);

        //2. 유저 상태 조회 불가시 에러처리
        ApiResponseDto badResponse = new ApiResponseDto(false, "유저 접속 상태 조회 불가", null);
        if (status.isEmpty()) return new ResponseEntity<>(badResponse, HttpStatus.BAD_REQUEST);

        ApiResponseDto goodResponse = new ApiResponseDto("유저 접속 상태 조회에 성공했습니다", status);
        return new ResponseEntity<>(goodResponse, HttpStatus.OK);
    }

    //# 회원 상태 변경
    // 'idle' : 채팅 종료시, 로그인시 자동으로 부여
    // 'matching' : 매칭 큐 진입
    // 'chatting' : 채팅 중
    @PostMapping("status")
    public ResponseEntity<?> changeUserStatus(@RequestHeader("X-User-ID") String serialNumber, @RequestBody String userInteractionState) {

        //1. 회원 상태 저장
        if(!userStatusService.saveUserStatus(serialNumber, userInteractionState)){
            ApiResponseDto badResponse = new ApiResponseDto(false, "유저 접속 상태 조회 불가", null);
            return new ResponseEntity<>(badResponse, HttpStatus.BAD_REQUEST);
        }


        ApiResponseDto goodResponse = new ApiResponseDto("유저 상태 변경에 성공했습니다", null);
        return new ResponseEntity<>(goodResponse, HttpStatus.OK);
    }

    //energy 1도 상승
    @PostMapping("energy")
    public ResponseEntity<?> increaseEnergy(@RequestBody EnergyRequest energyRequest) {

        //1. 유저 이름으로 검색
        Users user1 = userService.getUserBySerialNumber(energyRequest.getReceiverId());
        Users user2 = userService.getUserBySerialNumber(energyRequest.getSenderId());


        //2. 유저 energy 온도 하나 올려주기
        user1.setEnergy(user1.getEnergy() +1);
        user2.setEnergy(user2.getEnergy()+1);


        ApiResponseDto goodResponse = new ApiResponseDto("유저 에너지 상승에 성공했습니다", null);
        return new ResponseEntity<>(goodResponse, HttpStatus.OK);
    }


    // 쿠키 생성용 메서드
    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);

        //쿠키의 유효기간 설정
        cookie.setMaxAge(60*60 * 24 *7 ); //1분 *60 해야 1시간

        //SSL 통신채널 연결 시에만 쿠키를 전송하도록 설정
        cookie.setSecure(true);

        //브라우저가 쿠키값을 전송할 URL 지정
        cookie.setPath("/");
//        cookie.setDomain("i12a503.p.ssafy.io");

        //브라우저에서(javascript를 통해) 쿠키에 접근할 수 없도록 제한
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "None");


        return cookie;
    }


}


