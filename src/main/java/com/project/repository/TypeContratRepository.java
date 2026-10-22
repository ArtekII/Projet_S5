package com.project.repository;

import com.project.entity.TypeContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TypeContratRepository extends JpaRepository<TypeContrat, Integer> {
    Optional<TypeContrat> findByCode(String code);
}
