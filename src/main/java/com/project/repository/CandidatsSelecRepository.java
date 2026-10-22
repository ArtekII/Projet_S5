
package com.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.entity.CandidatsSelec;

@Repository
public interface CandidatsSelecRepository
        extends JpaRepository<CandidatsSelec, Integer> {

    /**
     * Toutes les etapes ({@code candidats_selec}) franchies par un candidat,
     * triees dans l'ordre chronologique : l'ordre de progression dans le
     * processus de recrutement.
     */
    @Query("""
        SELECT cs
        FROM CandidatsSelec cs
        JOIN FETCH cs.offreEmploi o
        JOIN FETCH o.poste
        JOIN FETCH o.departement
        JOIN FETCH cs.cvCandidat cv
        JOIN FETCH cs.etape
        WHERE cv.candidat.id = :idCandidat
        ORDER BY cs.dateSelection ASC, cs.etape.id ASC
    """)
    List<CandidatsSelec> findSelectionsByCandidatId(
            @Param("idCandidat") Integer idCandidat);
}
