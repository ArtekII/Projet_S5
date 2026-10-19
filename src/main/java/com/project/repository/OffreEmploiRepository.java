package com.project.repository;

import com.project.entity.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Integer> {
  List<OffreEmploi> findByStatut_CodeOrderByCreatedAtDesc(String code);

  List<OffreEmploi> findByStatut_CodeInOrderByCreatedAtDesc(List<String> codes);

  List<OffreEmploi> findByManager_IdOrderByCreatedAtDesc(Integer managerId);

  List<OffreEmploi> findAllByOrderByCreatedAtDesc();
}
