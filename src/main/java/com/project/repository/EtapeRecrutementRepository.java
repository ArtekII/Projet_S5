
package com.project.repository;

import java.util.List;
import java.util.Optional;

import com.project.entity.EtapeRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtapeRecrutementRepository extends JpaRepository<EtapeRecrutement, Integer> {

    Optional<EtapeRecrutement> findByCode(String code);

    /** Etapes du processus, dans l'ordre. */
    List<EtapeRecrutement> findAllByOrderByOrdreAsc();

    /** Etape suivante dans le processus de recrutement. */
    Optional<EtapeRecrutement> findFirstByOrdreGreaterThanOrderByOrdreAsc(Integer ordre);
}
