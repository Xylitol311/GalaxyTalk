package com.galaxytalk.auth.dto;

import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException; // 수정: OAuth2AuthenticationException import 추가

// 네이버 정보 받아오기
public class NaverResponse implements OAuth2Response {

    private final Map<String, Object> attribute;

    public NaverResponse(Map<String, Object> attribute) {
        // 수정: 응답 데이터 검증 추가
        if (attribute == null || !attribute.containsKey("response")) {
            throw new OAuth2AuthenticationException("네이버 응답에 필요한 response 값이 없습니다.");
        }
        Object responseObj = attribute.get("response");
        if (!(responseObj instanceof Map)) {
            throw new OAuth2AuthenticationException("네이버 응답의 response 값이 올바른 형식이 아닙니다.");
        }
        this.attribute = (Map<String, Object>) responseObj;
        if (!this.attribute.containsKey("id")) {
            throw new OAuth2AuthenticationException("네이버 응답에 필요한 id 값이 없습니다.");
        }
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getProviderId() {
        return attribute.get("id").toString();
    }
}
