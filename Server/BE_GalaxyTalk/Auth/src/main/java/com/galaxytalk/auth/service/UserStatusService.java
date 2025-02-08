package com.galaxytalk.auth.service;

import com.galaxytalk.auth.entity.UserStatus;
import com.galaxytalk.auth.repository.UserStatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserStatusRepository userStatusRepository;

    //userStatus 저장
    @Transactional
    public boolean saveUserStatus(String serialNumber, String status) {
        UserStatus userStatus =userStatusRepository.save(new UserStatus(serialNumber,status));
        if(userStatus==null) return false;
        else return true;
    }


    //userStatus 삭제
    @Transactional
    public void removeUserStatus(String serialNumber) {
        userStatusRepository.findById(serialNumber).
                ifPresent(x -> userStatusRepository.deleteById(x.getSerialNumber()));
    }

    public Map<String, String> getUserStatus(String userId) {
        Optional<UserStatus> userStatusOptional = userStatusRepository.findById(userId);

        Map<String, String> statusMap = new HashMap<>();

            UserStatus userStatus = userStatusOptional.get();
            statusMap.put("userInteractionState", userStatus.getUserInteractionState());

        return statusMap;
    }



}
