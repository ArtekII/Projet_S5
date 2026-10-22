package com.project.repository;

import com.project.entity.Etape;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EtapeRepository extends JpaRepository<Etape, Integer> {
    Optional<Etape> findByNom(String nom);
}
