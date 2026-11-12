package com.project.repository;

import com.project.entity.AlertePointage;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertePointageRepository extends JpaRepository<AlertePointage, Long> {
    List<AlertePointage> findByEmploye_IdAndDate(Integer employe, LocalDate date);
    List<AlertePointage> findByEmploye_IdAndResolueLeIsNullOrderByDateDesc(Integer employe);
}
