package com.galaxytalk.auth.repository;


import com.galaxytalk.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken,Long> {
    //dto 에서 @indexed 처리한 변수만 findby 사용가능
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

}

