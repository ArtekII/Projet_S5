package com.project.repository;

import com.project.entity.Planning;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningRepository extends JpaRepository<Planning, Integer> {
    Optional<Planning> findFirstByPoste_IdAndJourSemaineAndDateEffetLessThanEqualOrderByDateEffetDesc(Integer poste, int jour, LocalDate date);
    boolean existsByPoste_IdAndJourSemaineAndDateEffet(Integer poste, int jour, LocalDate date);
    List<Planning> findAllByOrderByPoste_IdAscJourSemaineAscDateEffetDesc();
}
