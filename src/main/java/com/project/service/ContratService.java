package com.project.service;

import org.springframework.stereotype.Service;
import com.project.entity.*;
import com.project.repository.*;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class ContratService {
    private final DelaiConditionsService delai;
    private final ContratRepository contratRepo;
    private final TypeContratRepository typeContratRepo;
    private final ConditionTravailRepository conditionRepo;
    private final ReponseConditionRepository reponseConditionRepo;
    private final CandidatureRepository candidatureRepo;
    private final ConditionTravailService conditionService;

    public ContratService(ContratRepository c, TypeContratRepository t, ConditionTravailRepository cond,
            ReponseConditionRepository r, CandidatureRepository candidatureRepo, ConditionTravailService conditionService, DelaiConditionsService delai) {
        this.delai = delai;
        this.contratRepo = c;
        this.typeContratRepo = t;
        this.conditionRepo = cond;
        this.reponseConditionRepo = r;
        this.candidatureRepo = candidatureRepo;
        this.conditionService = conditionService;
    }

    @Transactional(readOnly = true)
    public List<Contrat> contratsDuCandidat(Integer candidatId) {
        return contratRepo.findByCandidatId(candidatId);
    }

    @Transactional
    public Contrat accepterConditions(Integer candidatureId, Integer candidatId) {
        return accepter(candidatureId, candidatId, false);
    }

    @Transactional
    public Contrat accepterConditionsInitiales(Integer candidatureId, Integer candidatId) {
        return accepter(candidatureId, candidatId, true);
    }

    private Contrat accepter(Integer candidatureId, Integer candidatId, boolean initiales) {
        Map<String, Object> candidature = candidatureRepo.findPourContratForUpdate(candidatureId, candidatId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature inaccessible"));
        // Une seconde soumission renvoie le contrat deja cree.
        Contrat existant = contratRepo.findByIdCandidature(candidatureId).orElse(null);
        if (existant != null) return existant;
        if (!candidatureRepo.retenueApresEntretien(candidatureId)) {
            throw new IllegalStateException("La candidature doit etre retenue apres l'entretien");
        }
        if (!"CONDITIONS".equals(candidature.get("etape")) || !"EN_COURS".equals(candidature.get("statut"))) {
            throw new IllegalStateException("La candidature doit etre a l'etape Conditions et en cours");
        }
        ConditionTravail condition = initiales
                ? conditionService.reprendreConditionsInitiales(candidatureId)
                : conditionService.conditionsPourCandidature(candidatureId, (Integer) candidature.get("posteid"));
        boolean propositionRh = "RH".equals(condition.getRoleProposant().getCode()) && condition.getReponse() == null;
        boolean changementsValides = "CANDIDAT".equals(condition.getRoleProposant().getCode())
                && "VALIDEE".equals(condition.getDecisionRh().getCode()) && condition.getReponse() == null;
        if (!propositionRh && !changementsValides) {
            throw new IllegalStateException("Seules la proposition RH ou des modifications validees peuvent etre acceptees");
        }
        delai.verifier(condition);
        String code = Boolean.TRUE.equals(candidature.get("cdi_autorise")) ? "CDI" : "CE";
        TypeContrat type = typeContratRepo.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Type de contrat " + code + " introuvable"));
        condition.setReponse(reponseConditionRepo.findByCode("OUI")
                .orElseThrow(() -> new IllegalStateException("Reponse OUI introuvable")));
        conditionRepo.save(condition);
        Contrat contrat = new Contrat();
        contrat.setIdCandidature(candidatureId);
        contrat.setCondition(condition);
        contrat.setTypeContrat(type);
        contrat.setStatut("FINALISE");
        contrat = contratRepo.save(contrat);
        candidatureRepo.embaucher(candidatureId);
        return contrat;
    }
    @Transactional(readOnly = true)
    public List<Contrat> tousLesContrats() {
        return contratRepo.findAllByOrderByIdDesc();
    }

    @Transactional
    public void autoriserCdi(Integer candidatureId, Integer rhId) {
        Map<String, Object> candidature = candidatureRepo.verrouiller(candidatureId);
        verifierEnCours(candidature);
        if (contratRepo.findByIdCandidature(candidatureId).isPresent()) {
            throw new IllegalStateException("Le type ne peut plus changer apres creation du contrat");
        }
        candidatureRepo.autoriserCdi(candidatureId, rhId);
    }

    private void verifierEnCours(Map<String, Object> candidature) {
        if (!"CONDITIONS".equals(candidature.get("etape")) || !"EN_COURS".equals(candidature.get("statut"))) {
            throw new IllegalStateException("La candidature n'est plus a l'etape Conditions en cours");
        }
    }

}
