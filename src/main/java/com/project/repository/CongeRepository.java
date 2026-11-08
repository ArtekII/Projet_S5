package com.project.repository;

import com.project.entity.Conge;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CongeRepository extends JpaRepository<Conge, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conge c where c.id = :id")
    Optional<Conge> findByIdForUpdate(@Param("id") Integer id);
}
