package com.galaxytalk.auth.service;

import com.galaxytalk.auth.dto.CustomOAuth2User;
import com.galaxytalk.auth.dto.NaverResponse;
import com.galaxytalk.auth.dto.OAuth2Response;
import com.galaxytalk.auth.dto.UserDTO;
import com.galaxytalk.auth.entity.Planets;
import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.entity.Users;
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
    private final PlanetService planetService;

    public CustomOAuth2UserService(UserService userService, PlanetService planetService) {
        this.userService = userService;
        this.planetService = planetService;
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {


        System.out.println("OAUTHUSER에 옮!?!?!?");

        //Naver로 부터 전달 받은 값
        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println(userRequest.getAccessToken());

        OAuth2Response oAuth2Response = new NaverResponse(oAuth2User.getAttributes());

        //serialNumber로 유저 검색하기
        String userSerialNumber = oAuth2Response.getProviderId();
        Users user = userService.getUserBySerialNumber(userSerialNumber);

        Role role = null;

        // user 정보가 없을 경우 회원가입 처리
        if (user == null) {

            //NAVER로 부터 받은 데이터 setting
            Users newUser = new Users();
            newUser.setSerialNumber(userSerialNumber);
            newUser.setAgeInterval(oAuth2Response.getAgeInterval());
            newUser.setBirthday(oAuth2Response.getBirthday());
            newUser.setBirthyear(Integer.parseInt(oAuth2Response.getBirthyear()));
            newUser.setEmail(oAuth2Response.getEmail());

            //!!회원가입 페이지로 보내주기 -> 추후 로직 구현 필요
            //우선 임시로 회원 정보 내용 저장
            newUser.setMbti("ENTJ");
            Planets planet = planetService.getPlanetById(1);
            newUser.setPlanets(planet);

            //!!권한 추후에 분기처리하기
            role = Role.USER;
            newUser.setRole(role);

            //JPA 데이터 저장
            userService.saveUser(newUser);
        } else {

            role = user.getRole();
            System.out.println(user.toString());
            System.out.println(role.toString());

        }


        //리소스 서버에서 발급 받은 정보로 사용자를 특정할 아이디값을 만듬
        UserDTO userDTO = new UserDTO();
        userDTO.setName(oAuth2Response.getProviderId());
        userDTO.setRole("ROLE_USER");

        return new CustomOAuth2User(userDTO);
    }
}