
package com.project.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.CandidatsSelec;
import com.project.repository.CandidatsSelecRepository;

@Service
@Transactional(readOnly = true)
public class CandidatsSelecService {

    private final CandidatsSelecRepository candidatsSelecRepository;

    public CandidatsSelecService(
            CandidatsSelecRepository candidatsSelecRepository) {
        this.candidatsSelecRepository = candidatsSelecRepository;
    }

    public List<CandidatsSelec> getSelectionsByCandidatId(
            Integer idCandidat) {

        return candidatsSelecRepository
                .findSelectionsByCandidatId(idCandidat);
    }

    /**
     * Etapes d'un candidat regroupees par offre (cle = id de l'offre).
     * L'ordre des listes suit l'ordre chronologique des etapes.
     */
    public Map<Integer, List<CandidatsSelec>> getSelectionsParOffre(
            Integer idCandidat) {

        Map<Integer, List<CandidatsSelec>> parOffre = new LinkedHashMap<>();
        for (CandidatsSelec selection : getSelectionsByCandidatId(idCandidat)) {
            parOffre.computeIfAbsent(
                    selection.getOffreEmploi().getId(),
                    id -> new ArrayList<>())
                    .add(selection);
        }
        return parOffre;
    }
}
