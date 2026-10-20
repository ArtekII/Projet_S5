package com.project.repository;

import com.project.entity.CandidatSelec1;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidatSelec1Repository extends JpaRepository<CandidatSelec1, Integer> {
    List<CandidatSelec1> findByOffre_IdOrderByCvCandidat_AnneeExperienceDescCvCandidat_CreatedAtAsc(Integer offreId);
    boolean existsByOffre_IdAndCvCandidat_Id(Integer offreId, Integer cvId);
    List<CandidatSelec1> findByOffre_Id(Integer offreId);
}
