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
    private final EmployeService employes;
    private final ContratRepository contratRepo;
    private final TypeContratRepository typeContratRepo;
    private final ConditionTravailRepository conditionRepo;
    private final ReponseConditionRepository reponseConditionRepo;
    private final CandidatureJdbcRepository candidatureRepo;
    private final ConditionTravailService conditionService;

    public ContratService(ContratRepository c, TypeContratRepository t, ConditionTravailRepository cond,
            ReponseConditionRepository r, CandidatureJdbcRepository candidatureRepo, ConditionTravailService conditionService, DelaiConditionsService delai, EmployeService employes) {
        this.employes = employes;
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
    public ConditionTravail accepterConditions(Integer candidatureId, Integer candidatId) {
        return accepter(candidatureId, candidatId, false);
    }

    @Transactional
    public ConditionTravail accepterConditionsInitiales(Integer candidatureId, Integer candidatId) {
        return accepter(candidatureId, candidatId, true);
    }

    private ConditionTravail accepter(Integer candidatureId, Integer candidatId, boolean initiales) {
        Map<String, Object> candidature = candidatureRepo.findPourContratForUpdate(candidatureId, candidatId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature inaccessible"));
        Contrat existant = contratRepo.findByIdCandidature(candidatureId).orElse(null);
        if (existant != null) return existant.getCondition();
        verifierEnCours(candidature);
        if (!candidatureRepo.retenueApresEntretien(candidatureId)) {
            throw new IllegalStateException("La candidature doit etre retenue apres l'entretien");
        }
        ConditionTravail derniere = conditionRepo.findFirstByIdCandidatureOrderByVersionDesc(candidatureId).orElse(null);
        if (acceptee(derniere)) return derniere;
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
        condition.setReponse(reponseConditionRepo.findByCode("OUI")
                .orElseThrow(() -> new IllegalStateException("Reponse OUI introuvable")));
        return conditionRepo.save(condition);
    }

    private boolean acceptee(ConditionTravail condition) {
        return condition != null && condition.getReponse() != null && "OUI".equals(condition.getReponse().getCode());
    }

    @Transactional
    public Map<String, Object> dossierPourConfirmation(Integer candidatureId, Integer candidatId) {
        Map<String, Object> dossier = candidatureRepo.findPourContratForUpdate(candidatureId, candidatId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature inaccessible"));
        verifierEnCours(dossier);
        ConditionTravail condition = conditionRepo.findFirstByIdCandidatureOrderByVersionDesc(candidatureId).orElse(null);
        if (!acceptee(condition)) throw new IllegalStateException("Acceptez les conditions avant de confirmer le contrat.");
        dossier.put("id", candidatureId);
        dossier.put("condition", condition);
        return dossier;
    }

    @Transactional
    public Contrat confirmerContrat(Integer candidatureId, Integer candidatId, String choix) {
        Map<String, Object> candidature = candidatureRepo.findPourContratForUpdate(candidatureId, candidatId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature inaccessible"));
        Contrat existant = contratRepo.findByIdCandidature(candidatureId).orElse(null);
        if (existant != null) {
            employes.assurerPourContrat(existant);
            return existant;
        }
        verifierEnCours(candidature);
        if (!candidatureRepo.retenueApresEntretien(candidatureId)) {
            throw new IllegalStateException("La candidature doit etre retenue apres l'entretien");
        }
        ConditionTravail condition = conditionRepo.findFirstByIdCandidatureOrderByVersionDesc(candidatureId).orElse(null);
        if (!acceptee(condition)) throw new IllegalStateException("Acceptez les conditions avant de confirmer le contrat.");
        boolean accredite = Boolean.TRUE.equals(candidature.get("cdi_autorise"));
        if (accredite && (choix == null || choix.isBlank())) {
            throw new IllegalArgumentException("Choisissez votre type de contrat : CE ou CDI.");
        }
        String code = choix == null || choix.isBlank() ? "CE" : choix;
        if (!("CE".equals(code) || "CDI".equals(code))) {
            throw new IllegalArgumentException("Type de contrat invalide.");
        }
        if ("CDI".equals(code) && !accredite) {
            throw new IllegalArgumentException("Le CDI necessite une accreditation RH.");
        }
        TypeContrat type = typeContratRepo.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Type de contrat " + code + " introuvable"));

        Contrat contrat = new Contrat();
        contrat.setIdCandidature(candidatureId);
        contrat.setCondition(condition);
        contrat.setTypeContrat(type);
        contrat.setStatut("FINALISE");
        contrat = contratRepo.save(contrat);
        employes.assurerPourContrat(contrat);
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
