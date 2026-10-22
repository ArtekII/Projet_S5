package com.project.repository;

import com.project.entity.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CandidatureRepository extends JpaRepository<Candidature, Integer> {

    List<Candidature> findByCandidat_IdOrderByCreatedAtDesc(Integer userId);

    Optional<Candidature> findByOffre_IdAndCandidat_Id(Integer idOffre, Integer idCandidat);

    @Query("""
        SELECT c FROM Candidature c
        JOIN FETCH c.offre o JOIN FETCH o.poste JOIN FETCH o.departement
        JOIN FETCH c.statut JOIN FETCH c.etape
        WHERE c.candidat.id = :idCandidat ORDER BY c.createdAt DESC
    """)
    List<Candidature> findByCandidatIdAvecDetails(@Param("idCandidat") Integer idCandidat);

    @Query("""
        SELECT c FROM Candidature c
        JOIN FETCH c.offre o JOIN FETCH o.poste JOIN FETCH o.departement
        JOIN FETCH c.candidat JOIN FETCH c.statut JOIN FETCH c.etape
        WHERE c.statut.code = 'EN_COURS' ORDER BY c.createdAt DESC
    """)
    List<Candidature> findAllEnCoursAvecDetails();

    @Query("""
        SELECT c FROM Candidature c
        JOIN FETCH c.offre o JOIN FETCH o.poste JOIN FETCH o.departement
        JOIN FETCH c.candidat JOIN FETCH c.statut JOIN FETCH c.etape
        WHERE o.id = :idOffre ORDER BY c.createdAt DESC
    """)
    List<Candidature> findByOffreIdAvecDetails(@Param("idOffre") Integer idOffre);
}
