package com.project.repository;

import com.project.entity.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StatutOffreRepository extends JpaRepository<StatutOffre, Integer> {
  Optional<StatutOffre> findByCode(String code);
}
