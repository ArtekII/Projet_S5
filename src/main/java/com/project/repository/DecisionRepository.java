package com.project.repository;

import com.project.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DecisionRepository extends JpaRepository<Decision, Integer> {
  Optional<Decision> findByCode(String code);
}
