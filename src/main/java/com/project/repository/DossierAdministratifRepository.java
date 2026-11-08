package com.project.repository;

import com.project.entity.DossierAdministratif;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DossierAdministratifRepository extends JpaRepository<DossierAdministratif, Integer> {
    Optional<DossierAdministratif> findByUtilisateur_Id(Integer utilisateurId);
}
