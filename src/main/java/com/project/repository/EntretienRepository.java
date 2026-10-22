package com.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.entity.Entretien;

public interface EntretienRepository extends JpaRepository<Entretien, Integer> {
    Optional<Entretien> findByCandidature_Id(Integer idCandidature);
}
