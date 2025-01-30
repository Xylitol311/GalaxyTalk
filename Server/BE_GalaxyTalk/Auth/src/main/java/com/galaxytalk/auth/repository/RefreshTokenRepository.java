package com.galaxytalk.auth.repository;


import com.galaxytalk.auth.dto.RefreshTokenDTO;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshTokenDTO,Long> {

    //dto 에서 @indexed 처리한 변수만 findby 사용가능
    Optional<RefreshTokenDTO> findByRefreshToken(String refreshToken);

}

