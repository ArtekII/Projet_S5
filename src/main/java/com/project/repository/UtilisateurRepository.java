package com.project.repository;

import com.project.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
  Optional<Utilisateur> findByEmail(String email);
}
