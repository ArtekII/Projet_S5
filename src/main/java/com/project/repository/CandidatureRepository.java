
package com.project.repository;

import com.project.entity.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CandidatureRepository extends JpaRepository<Candidature, Integer> {

    List<Candidature> findByCandidat_IdOrderByCreatedAtDesc(Integer userId);

    /** Vrai si le candidat a deja postule a cette offre. */
    Optional<Candidature> findByOffre_IdAndCandidat_Id(Integer idOffre, Integer idCandidat);

    /**
     * Candidatures d'un candidat avec l'offre, le poste, le departement et le
     * statut charges (pour l'affichage de l'espace candidat).
     */
    @Query("""
        SELECT c
        FROM Candidature c
        JOIN FETCH c.offre o
        JOIN FETCH o.poste
        JOIN FETCH o.departement
        JOIN FETCH c.statut
        JOIN FETCH c.etape
        WHERE c.candidat.id = :idCandidat
        ORDER BY c.createdAt DESC
    """)
    List<Candidature> findByCandidatIdAvecDetails(
            @Param("idCandidat") Integer idCandidat);

    /**
     * Candidatures en cours (tous candidats) avec l'offre, le candidat et
     * l'etape charges : tableau de bord RH.
     */
    @Query("""
        SELECT c
        FROM Candidature c
        JOIN FETCH c.offre o
        JOIN FETCH o.poste
        JOIN FETCH o.departement
        JOIN FETCH c.candidat
        JOIN FETCH c.statut
        JOIN FETCH c.etape
        WHERE c.statut.code = 'EN_COURS'
        ORDER BY c.createdAt DESC
    """)
    List<Candidature> findAllEnCoursAvecDetails();
}
