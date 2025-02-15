package com.galaxytalk.auth.repository;

import com.galaxytalk.auth.entity.Planets;
import com.galaxytalk.auth.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanetRepository extends JpaRepository<Planets, Long> {

    Planets findById(int Id);
}