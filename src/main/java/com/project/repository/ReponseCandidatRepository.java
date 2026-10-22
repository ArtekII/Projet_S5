
package com.project.repository;

import java.util.List;
import java.util.Optional;

import com.project.entity.ReponseCandidat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReponseCandidatRepository extends JpaRepository<ReponseCandidat, Integer> {

    List<ReponseCandidat> findByCandidature_Id(Integer idCandidature);

    List<ReponseCandidat> findByCandidature_Offre_Id(Integer idOffre);

    Optional<ReponseCandidat> findByCandidature_IdAndQuestion_Id(Integer idCandidature,
            Integer idQuestion);
}
