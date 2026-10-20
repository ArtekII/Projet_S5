package com.project.repository;

import com.project.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Integer> {
    Optional<Contrat> findByIdCandidature(Integer candidatureId);

    @Query(value = "select co.* from contrat co join candidature c on c.id = co.id_candidature where c.id_candidat = :candidatId order by co.id desc", nativeQuery = true)
    List<Contrat> findByCandidatId(@Param("candidatId") Integer candidatId);
}
