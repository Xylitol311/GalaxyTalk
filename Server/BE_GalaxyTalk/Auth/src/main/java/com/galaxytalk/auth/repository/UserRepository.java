package com.galaxytalk.auth.repository;

import com.galaxytalk.auth.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Users findBySerialNumber(String serialNumber);


}