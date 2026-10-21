package com.project.service;

import org.springframework.stereotype.Service;
import com.project.entity.*;
import com.project.repository.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class ConditionTravailService {
    public List<ReponseCondition> reponsesConditions() {
        return List.of("OUI", "NON", "AUTRE").stream()
                .map(code -> reponseRepository.findByCode(code)
                        .orElseThrow(() -> new IllegalStateException("Reponse " + code + " introuvable")))
                .toList();
    }

    private final DelaiConditionsService delai;
    private final ConditionTravailRepository conditionTravailRepository;
    private final PosteRepository posteRepository;
    private final RoleRepository roleRepository;
    private final DecisionRepository decisionRepository;
    private final ReponseConditionRepository reponseRepository;
    private final CandidatureRepository candidatureRepository;

    public ConditionTravailService(ConditionTravailRepository conditionTravailRepository,
            PosteRepository posteRepository, RoleRepository roleRepository,
            DecisionRepository decisionRepository, ReponseConditionRepository reponseRepository,
            CandidatureRepository candidatureRepository, DelaiConditionsService delai) {
        this.delai = delai;
        this.conditionTravailRepository = conditionTravailRepository;
        this.posteRepository = posteRepository;
        this.roleRepository = roleRepository;
        this.decisionRepository = decisionRepository;
        this.reponseRepository = reponseRepository;
        this.candidatureRepository = candidatureRepository;
    }

    // Le RH definit une nouvelle version des conditions types d'un poste.
    @Transactional
    public ConditionTravail definirConditionsTravail(Poste poste, ConditionTravail donnees) {
        if (poste == null || poste.getId() == null) {
            throw new IllegalArgumentException("Un poste existant est obligatoire");
        }
        validerConditions(donnees);
        Poste existant = posteRepository.findByIdForUpdate(poste.getId())
                .orElseThrow(() -> new IllegalArgumentException("Poste introuvable"));
        int version = conditionTravailRepository.findFirstByPoste_IdOrderByVersionDesc(existant.getId())
                .map(c -> Math.addExact(c.getVersion(), 1)).orElse(1);
        ConditionTravail proposition = nouvelleProposition(donnees, "RH", version);
        proposition.setPoste(existant);
        return conditionTravailRepository.save(proposition);
    }

    // Une contre-proposition conserve la version RH rattachee a la candidature.
    @Transactional
    public ConditionTravail modifierConditionsTravail(Integer conditionId, ConditionTravail donnees) {
        if (conditionId == null) {
            throw new IllegalArgumentException("L'identifiant de la condition est obligatoire");
        }
        validerConditions(donnees);
        ConditionTravail precedente = conditionTravailRepository.findByIdForUpdate(conditionId)
                .orElseThrow(() -> new IllegalArgumentException("Conditions de travail introuvables"));
        if (precedente.getIdCandidature() == null || precedente.getPoste() != null) {
            throw new IllegalStateException("Une contre-proposition doit concerner une candidature");
        }
        ConditionTravail derniere = conditionTravailRepository
                .findFirstByIdCandidatureOrderByVersionDesc(precedente.getIdCandidature()).orElseThrow();
        if (!derniere.getId().equals(precedente.getId())) {
            throw new IllegalStateException("Cette proposition a deja ete remplacee par une nouvelle version");
        }
        if ((!"RH".equals(precedente.getRoleProposant().getCode()) && !contrePropositionRefusee(precedente)) || precedente.getReponse() != null) {
            throw new IllegalStateException("La proposition doit provenir du RH ou suivre une contre-proposition refusee");
        }
        delai.verifier(precedente);
        ConditionTravail proposition = nouvelleProposition(donnees, "CANDIDAT",
                Math.addExact(precedente.getVersion(), 1));
        proposition.setIdCandidature(precedente.getIdCandidature());
        if ("RH".equals(precedente.getRoleProposant().getCode())) {
            precedente.setReponse(reponseRepository.findByCode("AUTRE")
                    .orElseThrow(() -> new IllegalStateException("Reponse AUTRE introuvable")));
            conditionTravailRepository.save(precedente);
        }
        return conditionTravailRepository.save(proposition);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> postes() {
        List<Map<String, Object>> postes = new ArrayList<>();
        for (Poste poste : posteRepository.findByActifTrueOrderByIntituleAsc()) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("id", poste.getId());
            ligne.put("intitule", poste.getIntitule());
            ligne.put("conditions", conditionTravailRepository.findFirstByPoste_IdOrderByVersionDesc(poste.getId()).orElse(null));
            postes.add(ligne);
        }
        return postes;
    }

    @Transactional
    public List<Map<String, Object>> dossiers(Integer candidatId) {
        List<Map<String, Object>> dossiers = candidatureRepository.findDossiersConditions(candidatId);
        for (Map<String, Object> dossier : dossiers) {
            Integer candidatureId = (Integer) dossier.get("id");
            Map<String, Object> verrouillee = candidatureRepository.verrouiller(candidatureId);
            if (!"CONDITIONS".equals(verrouillee.get("etape")) || !"EN_COURS".equals(verrouillee.get("statut"))) continue;
            Integer posteId = (Integer) dossier.get("posteid");
            if (conditionTravailRepository.findFirstByPoste_IdOrderByVersionDesc(posteId).isPresent()) {
                conditionsPourCandidature(candidatureId, posteId);
            }
            List<ConditionTravail> historique = conditionTravailRepository.findByIdCandidatureOrderByVersionDesc(candidatureId);
            ConditionTravail modele = conditionTravailRepository.findFirstByPoste_IdOrderByVersionDesc((Integer) dossier.get("posteid")).orElse(null);
            dossier.put("expire", !historique.isEmpty() && delai.expire(historique.get(0)));
            dossier.put("historique", historique);
            dossier.put("modele", modele);
            dossier.put("actuelle", historique.isEmpty() ? modele : historique.get(0));
            dossier.put("initiale", historique.isEmpty() ? modele : historique.get(historique.size() - 1));
        }
        return dossiers;
    }

    @Transactional
    public void proposer(Integer candidatureId, Integer candidatId, ConditionTravail donnees) {
        validerConditions(donnees);
        Integer posteId = candidatureRepository.findPostePourNegociationForUpdate(candidatureId, candidatId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature inaccessible ou hors de l'etape Conditions"));
        ConditionTravail actuelle = conditionsPourCandidature(candidatureId, posteId);
        modifierConditionsTravail(actuelle.getId(), donnees);
    }

    // La candidature doit etre verrouillee par l'appelant avant cette copie initiale.
    ConditionTravail conditionsPourCandidature(Integer candidatureId, Integer posteId) {
        ConditionTravail actuelle = conditionTravailRepository.findFirstByIdCandidatureOrderByVersionDesc(candidatureId).orElse(null);
        if (actuelle == null) {
            ConditionTravail modele = conditionTravailRepository.findFirstByPoste_IdOrderByVersionDesc(posteId)
                    .orElseThrow(() -> new IllegalStateException("Le RH doit d'abord definir les conditions du poste"));
            actuelle = nouvelleProposition(modele, "RH", 1);
            actuelle.setIdCandidature(candidatureId);
            actuelle.setRepondreAvant(delai.nouvelleEcheance());
            actuelle = conditionTravailRepository.saveAndFlush(actuelle);
        }
        // Les propositions historiques recoivent un delai lors du deploiement.
        if (actuelle.getRepondreAvant() == null
                && !("CANDIDAT".equals(actuelle.getRoleProposant().getCode())
                     && "EN_ATTENTE".equals(actuelle.getDecisionRh().getCode()))) {
            actuelle.setRepondreAvant(delai.nouvelleEcheance());
        }
        return actuelle;
    }

    @Transactional
    public void publierConditionsDisponibles() {
        dossiers(null);
    }

    @Transactional
    public void prolonger(Integer candidatureId) {
        Map<String, Object> candidature = candidatureRepository.verrouiller(candidatureId);
        if (!"CONDITIONS".equals(candidature.get("etape")) || !"EN_COURS".equals(candidature.get("statut"))) {
            throw new IllegalStateException("La candidature n'est plus en negociation");
        }
        ConditionTravail actuelle = conditionTravailRepository
                .findFirstByIdCandidatureOrderByVersionDesc(candidatureId).orElseThrow(
                        () -> new IllegalStateException("Aucune condition individuelle disponible"));
        boolean enAttenteRh = "CANDIDAT".equals(actuelle.getRoleProposant().getCode())
                && "EN_ATTENTE".equals(actuelle.getDecisionRh().getCode());
        if (actuelle.getReponse() != null || enAttenteRh) {
            throw new IllegalStateException("Aucune reponse du candidat attendue");
        }
        java.time.LocalDateTime base = delai.maintenant();
        if (actuelle.getRepondreAvant() != null && actuelle.getRepondreAvant().isAfter(base)) {
            base = actuelle.getRepondreAvant();
        }
        // Ajouter un delai complet sans raccourcir une echeance existante.
        actuelle.setRepondreAvant(delai.prolonger(base));
        conditionTravailRepository.save(actuelle);
    }

    @Transactional
    public void decider(Integer conditionId, boolean valider) {
        Integer candidatureId = conditionTravailRepository.findCandidatureId(conditionId)
                .orElseThrow(() -> new IllegalArgumentException("Proposition individuelle introuvable"));
        Map<String, Object> candidature = candidatureRepository.verrouiller(candidatureId);
        if (!"CONDITIONS".equals(candidature.get("etape")) || !"EN_COURS".equals(candidature.get("statut"))) {
            throw new IllegalStateException("La candidature n'est plus en negociation");
        }
        ConditionTravail condition = conditionTravailRepository.findByIdForUpdate(conditionId).orElseThrow();
        if (condition.getIdCandidature() == null || !"CANDIDAT".equals(condition.getRoleProposant().getCode())
                || !"EN_ATTENTE".equals(condition.getDecisionRh().getCode())) {
            throw new IllegalStateException("Cette proposition n'est pas en attente de validation RH");
        }
        ConditionTravail derniere = conditionTravailRepository.findFirstByIdCandidatureOrderByVersionDesc(condition.getIdCandidature()).orElseThrow();
        if (!derniere.getId().equals(conditionId)) {
            throw new IllegalStateException("Cette proposition a ete remplacee");
        }
        condition.setDecisionRh(decisionRepository.findByCode(valider ? "VALIDEE" : "REFUSEE").orElseThrow());
        condition.setRepondreAvant(delai.nouvelleEcheance());
        conditionTravailRepository.save(condition);
    }

    private boolean contrePropositionRefusee(ConditionTravail condition) {
        return "CANDIDAT".equals(condition.getRoleProposant().getCode())
                && "REFUSEE".equals(condition.getDecisionRh().getCode());
    }

    // La candidature est verrouillee par ContratService.
    ConditionTravail reprendreConditionsInitiales(Integer candidatureId) {
        ConditionTravail derniere = conditionTravailRepository
                .findFirstByIdCandidatureOrderByVersionDesc(candidatureId)
                .orElseThrow(() -> new IllegalStateException("Aucune proposition individuelle"));
        if (!contrePropositionRefusee(derniere) || derniere.getReponse() != null) {
            throw new IllegalStateException("La derniere contre-proposition doit avoir ete refusee par le RH");
        }
        delai.verifier(derniere);
        List<ConditionTravail> historique = conditionTravailRepository
                .findByIdCandidatureOrderByVersionDesc(candidatureId);
        ConditionTravail initiale = historique.get(historique.size() - 1);
        if (!"RH".equals(initiale.getRoleProposant().getCode())) {
            throw new IllegalStateException("Proposition RH initiale introuvable");
        }
        ConditionTravail reprise = nouvelleProposition(initiale, "RH", Math.addExact(derniere.getVersion(), 1));
        reprise.setIdCandidature(candidatureId);
        reprise.setRepondreAvant(derniere.getRepondreAvant());
        return conditionTravailRepository.save(reprise);
    }

    private ConditionTravail nouvelleProposition(ConditionTravail donnees, String role, int version) {
        ConditionTravail proposition = new ConditionTravail();
        proposition.setVersion(version);
        proposition.setRoleProposant(roleRepository.findByCode(role)
                .orElseThrow(() -> new IllegalStateException("Role " + role + " introuvable")));
        proposition.setDecisionRh(decisionRepository.findByCode("EN_ATTENTE")
                .orElseThrow(() -> new IllegalStateException("Decision EN_ATTENTE introuvable")));
        proposition.setSalaire(donnees.getSalaire());
        proposition.setHeuresParSemaine(donnees.getHeuresParSemaine());
        proposition.setHoraires(donnees.getHoraires());
        proposition.setLieuTravail(donnees.getLieuTravail());
        proposition.setRemarque(donnees.getRemarque());
        return proposition;
    }

    private void validerConditions(ConditionTravail donnees) {
        if (donnees == null) {
            throw new IllegalArgumentException("Les conditions de travail sont obligatoires");
        }
        validerNombre(donnees.getSalaire(), 10, 2, false, "Salaire");
        validerNombre(donnees.getHeuresParSemaine(), 3, 1, true, "Heures par semaine");
        if (donnees.getLieuTravail() == null || donnees.getLieuTravail().isBlank()
                || donnees.getLieuTravail().length() > 255) {
            throw new IllegalArgumentException("Le lieu de travail est obligatoire (255 caracteres maximum)");
        }
        if (donnees.getHoraires() != null && donnees.getHoraires().length() > 100) {
            throw new IllegalArgumentException("Les horaires sont limites a 100 caracteres");
        }
    }

    private void validerNombre(BigDecimal valeur, int entiers, int decimales, boolean strictementPositif,
            String champ) {
        if (valeur == null || valeur.signum() < 0 || (strictementPositif && valeur.signum() == 0)
                || valeur.compareTo(BigDecimal.TEN.pow(entiers)) >= 0
                || valeur.stripTrailingZeros().scale() > decimales) {
            throw new IllegalArgumentException(champ + " invalide pour le schema SQL");
        }
    }

    @Transactional
    public void refuser(Integer candidatureId, Integer candidatId) {
        Integer posteId = candidatureRepository.findPostePourNegociationForUpdate(candidatureId, candidatId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature inaccessible ou hors de l'etape Conditions"));
        ConditionTravail condition = conditionsPourCandidature(candidatureId, posteId);
        boolean propositionRh = "RH".equals(condition.getRoleProposant().getCode());
        boolean changementsValides = "CANDIDAT".equals(condition.getRoleProposant().getCode())
                && "VALIDEE".equals(condition.getDecisionRh().getCode());
        if (condition.getReponse() != null || (!propositionRh && !changementsValides && !contrePropositionRefusee(condition))) {
            throw new IllegalStateException("Seule une proposition RH ou des changements valides peuvent etre refuses");
        }
        delai.verifier(condition);
        condition.setReponse(reponseRepository.findByCode("NON")
                .orElseThrow(() -> new IllegalStateException("Reponse NON introuvable")));
        conditionTravailRepository.save(condition);
        candidatureRepository.refuserParCandidat(candidatureId);
    }
}
