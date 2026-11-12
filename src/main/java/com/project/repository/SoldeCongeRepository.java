package com.project.repository;

import com.project.entity.SoldeConge;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from SoldeConge s
            where s.employe.id = :employeId
              and s.typeConge.id = :typeCongeId
              and s.periodeDebut = :periodeDebut
            """)
    Optional<SoldeConge> findForUpdate(@Param("employeId") Integer employeId,
            @Param("typeCongeId") Integer typeCongeId,
            @Param("periodeDebut") LocalDate periodeDebut);
}
