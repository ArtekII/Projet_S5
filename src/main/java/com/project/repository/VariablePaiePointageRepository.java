package com.project.repository;

import com.project.entity.VariablePaiePointage;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariablePaiePointageRepository extends JpaRepository<VariablePaiePointage, Long> {
    Optional<VariablePaiePointage> findByEmploye_IdAndDate(Integer employe, LocalDate date);
    List<VariablePaiePointage> findByDateBetweenOrderByDateAscEmploye_IdAsc(LocalDate du, LocalDate au);
}
