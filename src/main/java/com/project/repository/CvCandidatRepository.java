
package com.project.repository;

import java.util.List;
import java.util.Optional;

import com.project.entity.CvCandidat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CvCandidatRepository extends JpaRepository<CvCandidat, Integer> {

    Optional<CvCandidat> findByCandidat_IdAndOffreEmploi_Id(Integer idCandidat,
            Integer idOffre);

    List<CvCandidat> findByCandidat_Id(Integer idCandidat);
}
