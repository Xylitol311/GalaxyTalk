package com.galaxytalk.auth.service;

import com.galaxytalk.auth.entity.Planets;
import com.galaxytalk.auth.repository.PlanetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanetService {

    private final PlanetRepository planetRepository;

    // 모든 행성 조회
    public List<Planets> getAllPlanets() {
        return planetRepository.findAll();
    }

    // ID로 행성 조회
    public Planets getPlanetById(int id) {
        return planetRepository.findById(id);
    }

}
