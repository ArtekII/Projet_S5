package com.project.repository;

import com.project.entity.CorrectionPointage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionPointageRepository extends JpaRepository<CorrectionPointage, Long> {
    List<CorrectionPointage> findByPointage_Employe_IdAndPointage_DateOrderByIdDesc(Integer employe, java.time.LocalDate date);
}
