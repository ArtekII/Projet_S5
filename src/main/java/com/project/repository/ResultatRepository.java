
package com.project.repository;

import java.util.Optional;

import com.project.entity.Resultat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultatRepository extends JpaRepository<Resultat, Integer> {

    Optional<Resultat> findByCode(String code);
}
