package com.galaxytalk.auth.dto;

import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException; // 수정: OAuth2AuthenticationException import 추가

public class KakaoResponse implements OAuth2Response {

    private final Map<String, Object> attribute;

    public KakaoResponse(Map<String, Object> attribute) {
        // 수정: 응답 데이터 검증 추가
        if (attribute == null || !attribute.containsKey("id")) {
            throw new OAuth2AuthenticationException("카카오 응답에 필요한 id 값이 없습니다.");
        }
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