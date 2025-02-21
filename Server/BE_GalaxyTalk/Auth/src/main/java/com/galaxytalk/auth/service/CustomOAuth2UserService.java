package com.galaxytalk.auth.service;

import com.galaxytalk.auth.dto.*;
import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.entity.Users;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/*
로그인 성공 후 데이터를 전달 받기 위한 구현체 (작성하지 않을 경우 오류 발생)
어떤 데이터를 받을 건지, 어떻게 받을건지는 직접 설정해줘야함
*/

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    //DefaultOAuth2UserService OAuth2UserService의 구현체

    private final UserService userService;

    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {


        //Naver로 부터 전달 받은 값
        OAuth2User oAuth2User = super.loadUser(userRequest);

        System.out.println("provider로 부터 값 받는 중");

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        System.out.println(registrationId);

        OAuth2Response oAuth2Response = null;

        if(registrationId.equals("naver")) {

            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        }
        else if(registrationId.equals("kakao")){
            oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());
        }
        //serialNumber로 유저 검색하기
        String userSerialNumber = oAuth2Response.getProviderId();
        Users user = userService.getUserBySerialNumber(userSerialNumber);

        System.out.println("유저 검색 시도 완료");

        // user 정보가 없을 경우 회원가입 처리
        if (user == null) {

            System.out.println("회원가입 처리 시작");

            user = new Users();
            user.setSerialNumber(userSerialNumber);

            //!!권한 추후에 분기처리하기
            Role role = Role.ROLE_GUEST;
            user.setRole(role);

            //데이터 저장
            try {
                // 중복 등록 방지를 위한 try-catch 추가
                userService.saveUser(user);
            } catch (DataIntegrityViolationException e) {
                System.out.println("중복 회원가입 시도 감지, 기존 사용자 재조회");
                user = userService.getUserBySerialNumber(userSerialNumber);
            }
        }

        System.out.println("UserDTO 만들어소 CustomeOAuth2User에 넣어주기");

        //리소스 서버에서 발급 받은 정보로 사용자를 특정할 아이디값을 만듬
        UserDTO userDTO = new UserDTO();
        userDTO.setName(oAuth2Response.getProviderId());
        userDTO.setRole(user.getRole().toString());

        return new CustomOAuth2User(userDTO);
    }
}