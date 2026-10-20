package com.project.repository;

import com.project.entity.CvCandidat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CvCandidatRepository extends JpaRepository<CvCandidat, Integer> {
    boolean existsByCandidat_IdAndOffre_Id(Integer candidatId, Integer offreId);

    Optional<CvCandidat> findByCandidat_IdAndOffre_Id(Integer candidatId, Integer offreId);
}
