package com.galaxytalk.auth.service;


import com.galaxytalk.auth.entity.Users;
import com.galaxytalk.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 모든 유저 조회
    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }


    // Serial Number로 유저 조회
    public Users getUserBySerialNumber(String serialNumber) {

        return userRepository.findBySerialNumber(serialNumber);
    }

    // 유저 저장
    @Transactional
    public Users saveUser(Users user) {
        return userRepository.save(user);
    }



}
