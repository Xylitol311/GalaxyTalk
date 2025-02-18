package com.galaxytalk.auth.service;


import com.galaxytalk.auth.dto.ApiResponseDto;
import com.galaxytalk.auth.entity.Planets;
import com.galaxytalk.auth.entity.Role;
import com.galaxytalk.auth.entity.Users;
import com.galaxytalk.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PlanetService planetService;

    // 모든 유저 조회
    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }


    // Serial Number로 유저 조회
    @Transactional(readOnly = true)
    public Users getUserBySerialNumber(String serialNumber) {

        return userRepository.findBySerialNumber(serialNumber);
    }

    //유저 저장
    @Transactional
    public Users saveUser(Users user) {

        return userRepository.save(user);
    }


    //유저 update 및 회원가입
    @Transactional
    public Boolean signup(String serialNumber,String mbti, int planetId){
        Users user = userRepository.findBySerialNumber(serialNumber);
        Planets planet = planetService.getPlanetById(planetId);

        if(user==null || planet == null)
            return false;

        user.setRole(Role.ROLE_USER);
        user.setMbti(mbti);
        user.setPlanets(planet);

        return true;

    }


    //유저 탙퇴
    @Transactional
    public Boolean withDraw(String serialNumber){
        Users user = userRepository.findBySerialNumber(serialNumber);

        if(user==null)
            return false;

        user.setWithdrawnAt(LocalDateTime.now());

        user.setRole(Role.ROLE_WITHDRAW);

        return true;

    }





}
