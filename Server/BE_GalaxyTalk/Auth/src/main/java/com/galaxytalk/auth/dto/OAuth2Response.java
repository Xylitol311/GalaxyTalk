package com.galaxytalk.auth.dto;

public interface OAuth2Response {

    //제공자 (Ex. naver, google, ...)
    String getProvider();
    //제공자에서 발급해주는 아이디(번호)
    String getProviderId();
    //이메일
    String getEmail();

    //사용자 나이구간
    String getAgeInterval();
    //사용자 생일(일자)ex)  birthday=06-26
    String getBirthday();

    //사용자 생일(년도)ex)  birthyear=1998
    String getBirthyear();
}