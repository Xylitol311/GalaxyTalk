package com.galaxytalk.auth.repository;

import com.galaxytalk.auth.entity.UserStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatusRepository extends CrudRepository<UserStatus, String> {

    @Override
    Optional<UserStatus> findById(String serialNumber);
}