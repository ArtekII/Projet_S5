package com.project.repository;

import com.project.entity.CandidatSelec;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CandidatSelecRepository extends JpaRepository<CandidatSelec, Integer> {
    List<CandidatSelec> findByOffre_IdAndEtape_NomOrderByCvCandidat_AnneeExperienceDescCvCandidat_CreatedAtAsc(
            Integer offreId, String etape);

    List<CandidatSelec> findByOffre_IdAndEtape_NomOrderByNoteDesc(Integer offreId, String etape);

    List<CandidatSelec> findByOffre_IdAndEtape_NomOrderByRangAsc(Integer offreId, String etape);

    Optional<CandidatSelec> findByOffre_IdAndCvCandidat_IdAndEtape_Nom(
            Integer offreId, Integer cvId, String etape);
}
