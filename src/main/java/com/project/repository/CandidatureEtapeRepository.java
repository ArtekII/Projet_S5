
package com.project.repository;

import java.util.List;

import com.project.entity.CandidatureEtape;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidatureEtapeRepository extends JpaRepository<CandidatureEtape, Integer> {

    /** Historique des etapes franchies par une candidature. */
    List<CandidatureEtape> findByCandidature_IdOrderByCreatedAtAsc(Integer idCandidature);
}
