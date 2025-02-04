package com.galaxytalk.auth.dto;

import java.util.Map;

public class NaverResponse implements OAuth2Response{

    private final Map<String, Object> attribute;

    // 네이버 데이터 형태 예시
    //{resultcode=00, message=success, response={id=stringvaluexample, age=20-29, email=ss@naver.com, birthday=06-26, birthyear=1998}}
    public NaverResponse(Map<String, Object> attribute) {

        this.attribute = (Map<String, Object>) attribute.get("response");
    }

    @Override
    public String getProvider() {

        return "naver";
    }

    @Override
    public String getProviderId() {

        return attribute.get("id").toString();
    }

    @Override
    public String getEmail() {

        return attribute.get("email").toString();
    }

    @Override
    public String getAgeInterval() {

        return attribute.get("age").toString();
    }

    @Override
    public String getBirthday() {

        return attribute.get("birthday").toString();

    }

    @Override
    public String getBirthyear() {
        return attribute.get("birthyear").toString();
    }


}