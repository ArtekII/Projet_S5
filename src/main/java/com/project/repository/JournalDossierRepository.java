package com.project.repository;

import com.project.entity.JournalDossier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalDossierRepository extends JpaRepository<JournalDossier, Long> {
    List<JournalDossier> findTop100ByUtilisateur_IdOrderByIdDesc(Integer utilisateurId);
}
