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

    // ID로 유저 조회
    public Optional<Users> getUserById(Long id) {

        return userRepository.findById(id);
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

//    // 유저 정보 업데이트
//    @Transactional
//    public Users updateUser(Long id, Users updatedUser) {
//        return userRepository.findById(id)
//                .map(user -> {
//                    user.setSerialNumber(updatedUser.getSerialNumber());
//                    user.setUsername(updatedUser.getUsername());
//                    user.setEmail(updatedUser.getEmail());
//                    // 추가 필드 업데이트 가능
//                    return userRepository.save(user);
//                }).orElseThrow(() -> new RuntimeException("User not found"));
//    }

//    // 유저 삭제(Soft delete 처리)
//    @Transactional
//    public void deleteUser(Long id) {
//
//        userRepository.deleteById(id);
//    }


}
