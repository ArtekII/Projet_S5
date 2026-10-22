
package com.project.repository;

import java.util.List;

import com.project.entity.Etape;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtapeRepository extends JpaRepository<Etape, Integer> {

    /** Catalogue des etapes du processus de recrutement (table {@code etape}). */
    List<Etape> findAllByOrderByIdAsc();
}
