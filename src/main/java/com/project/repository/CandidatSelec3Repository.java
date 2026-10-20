package com.project.repository;

import com.project.entity.CandidatSelec3;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidatSelec3Repository extends JpaRepository<CandidatSelec3, Integer> {
    List<CandidatSelec3> findByCandidatSelec2_CandidatSelec1_Offre_IdOrderByRangAsc(Integer offreId);
}
