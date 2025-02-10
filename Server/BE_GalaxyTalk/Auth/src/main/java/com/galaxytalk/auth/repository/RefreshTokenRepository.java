package com.galaxytalk.auth.repository;


import com.galaxytalk.auth.entity.RefreshToken;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {  // Entity와 ID 타입 일치시킴

    @Override
    Optional<RefreshToken> findById(String accessToken);



}

