package com.galaxytalk.auth.dto;

import java.util.Map;

public class KakaoResponse implements OAuth2Response{

    private Map<String, Object> attribute;

    public KakaoResponse(Map<String, Object> attribute) {
        this.attribute = attribute;

    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return attribute.get("id").toString();
    }

}
