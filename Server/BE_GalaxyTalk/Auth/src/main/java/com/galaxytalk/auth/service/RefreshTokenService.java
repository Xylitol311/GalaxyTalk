package com.galaxytalk.auth.service;

import com.galaxytalk.auth.entity.RefreshToken;
import com.galaxytalk.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    //사용자 식별 아이디, 리프레시 토큰, 엑세스 토큰 저장
    @Transactional
    public void saveTokenInfo(String accessToken, String refreshToken) {
        refreshTokenRepository.save(new RefreshToken(accessToken,refreshToken));
    }


    //리프레시 토큰 삭제
    @Transactional
    public Boolean removeRefreshToken(String accessToken) {
        if(!refreshTokenRepository.findById(accessToken).isPresent())
            return false;


      refreshTokenRepository.deleteById(accessToken);
        return true;
    }

    public String getRefreshToken(String accessToken){

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findById(accessToken);

        return refreshToken.get().getRefreshToken();
    }



}

