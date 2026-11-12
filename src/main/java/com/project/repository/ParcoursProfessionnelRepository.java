package com.project.repository;

import com.project.entity.ParcoursProfessionnel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParcoursProfessionnelRepository extends JpaRepository<ParcoursProfessionnel, Integer> {
    List<ParcoursProfessionnel> findByUtilisateur_IdOrderByDateDebutDescIdDesc(Integer utilisateurId);
}
