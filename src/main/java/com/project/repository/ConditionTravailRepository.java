package com.project.repository;

import com.project.entity.ConditionTravail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface ConditionTravailRepository extends JpaRepository<ConditionTravail, Integer> {
    List<ConditionTravail> findByIdCandidatureOrderByVersionDesc(Integer candidatureId);

    Optional<ConditionTravail> findFirstByPoste_IdOrderByVersionDesc(Integer posteId);

    Optional<ConditionTravail> findFirstByIdCandidatureOrderByVersionDesc(Integer candidatureId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConditionTravail c where c.id = :id")
    Optional<ConditionTravail> findByIdForUpdate(@Param("id") Integer id);
}
