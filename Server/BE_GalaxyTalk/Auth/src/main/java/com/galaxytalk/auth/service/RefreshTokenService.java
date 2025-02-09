package com.galaxytalk.auth.service;

import com.galaxytalk.auth.entity.RefreshToken;
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
    public void saveTokenInfo(String refreshToken) {
        refreshTokenRepository.save(new RefreshToken(refreshToken));
    }


    //리프레시 토큰 삭제
    @Transactional
    public void removeRefreshToken(String refreshToken) {

        refreshTokenRepository.findByRefreshToken(refreshToken)
                .ifPresent(x -> refreshTokenRepository.deleteById(x.getId()));

    }

    public Boolean findRefreshToken(String refreshToken) {
        if(!refreshTokenRepository.findByRefreshToken(refreshToken).isPresent())
            return false;

        return true;

    }


}

