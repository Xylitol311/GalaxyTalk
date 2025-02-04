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

    // 행성 저장
    @Transactional
    public Planets savePlanet(Planets planet) {
        return planetRepository.save(planet);
    }

    // 행성 정보 업데이트
//    @Transactional
//    public Planets updatePlanet(Long id, Planets updatedPlanet) {
//        return planetRepository.findById(id)
//                .map(planet -> {
//                    planet.setName(updatedPlanet.getName());
//                    planet.setDescription(updatedPlanet.getDescription());
//                    // 추가 필드 업데이트 가능
//                    return planetRepository.save(planet);
//                }).orElseThrow(() -> new RuntimeException("Planet not found"));
//    }

    // 행성 삭제
    @Transactional
    public void deletePlanet(Long id) {
        planetRepository.deleteById(id);
    }
}
