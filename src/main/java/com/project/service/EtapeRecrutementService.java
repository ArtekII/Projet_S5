
package com.project.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.entity.EtapeRecrutement;
import com.project.repository.EtapeRecrutementRepository;

/** Etapes du processus de recrutement (Selection des CV, Questionnaire, ...). */
@Service
public class EtapeRecrutementService {

    private final EtapeRecrutementRepository etapeRecrutementRepository;

    public EtapeRecrutementService(EtapeRecrutementRepository etapeRecrutementRepository) {
        this.etapeRecrutementRepository = etapeRecrutementRepository;
    }

    /** Etapes du processus, dans l'ordre. */
    public List<EtapeRecrutement> getEtapes() {
        return etapeRecrutementRepository.findAllByOrderByOrdreAsc();
    }
}
