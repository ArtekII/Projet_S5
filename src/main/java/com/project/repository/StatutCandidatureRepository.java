
package com.project.repository;

import java.util.Optional;

import com.project.entity.StatutCandidature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatutCandidatureRepository extends JpaRepository<StatutCandidature, Integer> {

    Optional<StatutCandidature> findByCode(String code);
}
