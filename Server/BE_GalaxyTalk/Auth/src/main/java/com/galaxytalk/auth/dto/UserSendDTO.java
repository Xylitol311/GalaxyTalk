package com.galaxytalk.auth.dto;

import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// 클라이언트 유저 정보 전달용 DTO
@Getter
public class UserSendDTO {
    private String userId;
    private String mbti;
    private int energy;
    private Role role;
    private int planetId;  // ✅ Planets 엔티티 대신 ID만 반환

    public UserSendDTO(String userId, String mbti, int energy, Role role, int PlanetId){
        this.userId = userId;
        this.mbti = mbti;
        this.energy = energy;
        this.role= role;
        this.planetId = PlanetId;
    }

}
