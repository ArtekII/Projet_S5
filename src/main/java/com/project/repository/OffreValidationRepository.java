package com.project.repository;

import com.project.entity.OffreValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OffreValidationRepository extends JpaRepository<OffreValidation, Integer> {
  List<OffreValidation> findByOffre_IdOrderByDecidedAtAsc(Integer offreId);
}
