package com.project.repository;

import com.project.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Integer> {
    @Query("select c.idCandidature from Contrat c where c.id = :id")
    Optional<Integer> findCandidatureId(@Param("id") Integer id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contrat c where c.id = :id")
    Optional<Contrat> findByIdForUpdate(@Param("id") Integer id);

    List<Contrat> findAllByOrderByIdDesc();

    Optional<Contrat> findByIdCandidature(Integer candidatureId);

    @Query(value = "select co.* from contrat co join candidature c on c.id = co.id_candidature where c.id_candidat = :candidatId order by co.id desc", nativeQuery = true)
    List<Contrat> findByCandidatId(@Param("candidatId") Integer candidatId);
}
