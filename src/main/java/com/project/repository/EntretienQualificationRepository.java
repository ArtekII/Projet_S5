package com.project.repository;

import com.project.entity.EntretienQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntretienQualificationRepository extends JpaRepository<EntretienQualification, Integer> {
    List<EntretienQualification> findByOffre_IdOrderByIdAsc(Integer offreId);
}
