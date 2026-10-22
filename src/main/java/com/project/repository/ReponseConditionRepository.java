package com.project.repository;

import com.project.entity.ReponseCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReponseConditionRepository extends JpaRepository<ReponseCondition, Integer> {
    Optional<ReponseCondition> findByCode(String code);
}
