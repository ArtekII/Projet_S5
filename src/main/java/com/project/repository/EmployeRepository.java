package com.project.repository;

import com.project.entity.Employe;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeRepository extends JpaRepository<Employe, Integer> {
    Optional<Employe> findByUtilisateurId(Integer utilisateurId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employe e where e.id = :id")
    Optional<Employe> findByIdForUpdate(@Param("id") Integer id);
}
