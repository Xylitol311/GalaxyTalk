package com.galaxytalk.auth.service;

import com.galaxytalk.auth.dto.RefreshTokenDTO;
import com.galaxytalk.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    //사용자 식별 아이디, 리프레시 토큰, 엑세스 토큰 저장
    @Transactional
    public void saveTokenInfo(String refreshToken, String accessToken) {
        refreshTokenRepository.save(new RefreshTokenDTO(refreshToken));
    }

    //리프레시 토큰 삭제
    @Transactional
    public void removeRefreshToken(String refreshToken) {

        refreshTokenRepository.findByRefreshToken(refreshToken)
                .ifPresent(x -> refreshTokenRepository.deleteById(x.getId()));
    }


//    //리프레시 토큰 가져오기 (액세스 토큰으로 가져오기)
//    @Transactional
//    public RefreshTokenDTO getTokenInfoByaccessToken(String accessToken){
//        RefreshTokenDTO refreshToken = refreshTokenRepository.findByAccessToken(accessToken)
//                .orElseThrow(() -> new RuntimeException("없다!"));
//
//        return refreshToken;
//    }
//
//    //리프레시 토큰 가져오기 (아이디로 가져오기)
//    @Transactional
//    public RefreshTokenDTO getTokenInfoById(String id){
//        RefreshTokenDTO refreshToken = refreshTokenRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("없다!"));
//
//        return refreshToken;
//    }
}

