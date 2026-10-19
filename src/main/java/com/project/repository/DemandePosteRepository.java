package com.project.repository;

import com.project.entity.DemandePoste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DemandePosteRepository extends JpaRepository<DemandePoste, Integer> {
  List<DemandePoste> findByDecision_CodeOrderByCreatedAtDesc(String code);

  List<DemandePoste> findByManager_IdOrderByCreatedAtDesc(Integer managerId);

  List<DemandePoste> findAllByOrderByCreatedAtDesc();
}
