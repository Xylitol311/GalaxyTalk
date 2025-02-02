package com.galaxytalk.auth.dto;

import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.entity.Users;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserSend {
    private Long id;
    private String serialNumber;
    private String email;
    private String ageInterval;
    private String birthday;
    private int birthyear;
    private String mbti;
    private int energy;
    private int numberOfBlocks;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime withdrawnAt;
    private int planetId;  // ✅ Planets 엔티티 대신 ID만 반환

    public UserSend(Users user) {
        this.id = user.getId();
        this.serialNumber = user.getSerialNumber();
        this.email = user.getEmail();
        this.ageInterval = user.getAgeInterval();
        this.birthday = user.getBirthday();
        this.birthyear = user.getBirthyear();
        this.mbti = user.getMbti();
        this.energy = user.getEnergy();
        this.numberOfBlocks = user.getNumberOfBlocks();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.withdrawnAt = user.getWithdrawnAt();
        this.planetId = user.getPlanets() != null ? user.getPlanets().getId() : null;  // ✅ Planets가 null이면 null 반환
    }
}
