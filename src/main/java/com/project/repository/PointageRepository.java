package com.project.repository;

import com.project.entity.Pointage;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PointageRepository extends JpaRepository<Pointage, Long> {
    Optional<Pointage> findByJeton(String jeton);
    @Query("select p.employe.id from Pointage p where p.jeton = :jeton")
    Optional<Integer> employePourJeton(String jeton);
    @Query("select p.employe.id from Pointage p where p.id = :id")
    Optional<Integer> employePourPointage(Long id);
    List<Pointage> findByEmploye_IdAndDateOrderByConnexionAscIdAsc(Integer employe, LocalDate date);
    @Query("select distinct p.date from Pointage p where p.employe.id = :employe and p.deconnexion is null and p.regularise = false")
    List<LocalDate> datesOuvertes(Integer employe);
}
