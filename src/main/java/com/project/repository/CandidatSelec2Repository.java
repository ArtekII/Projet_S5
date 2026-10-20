package com.project.repository;

import com.project.entity.CandidatSelec2;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CandidatSelec2Repository extends JpaRepository<CandidatSelec2, Integer> {
    List<CandidatSelec2> findByCandidatSelec1_Offre_IdOrderByPointsDesc(Integer offreId);
    Optional<CandidatSelec2> findByCandidatSelec1_Id(Integer selection1Id);
}
