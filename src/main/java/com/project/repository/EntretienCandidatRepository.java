package com.project.repository;

import com.project.entity.EntretienCandidat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntretienCandidatRepository extends JpaRepository<EntretienCandidat, Integer> {
    List<EntretienCandidat> findByCandidatSelec2_Id(Integer selection2Id);
}
