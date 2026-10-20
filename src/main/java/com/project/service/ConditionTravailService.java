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

    private final ConditionTravailRepository conditionTravailRepository;
    private final PosteRepository posteRepository;
    private final RoleRepository roleRepository;
    private final DecisionRepository decisionRepository;
    private final ReponseConditionRepository reponseRepository;
    private final CandidatureRepository candidatureRepository;

    public ConditionTravailService(ConditionTravailRepository conditionTravailRepository,
            PosteRepository posteRepository, RoleRepository roleRepository,
            DecisionRepository decisionRepository, ReponseConditionRepository reponseRepository,
            CandidatureRepository candidatureRepository) {
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
        if (!"RH".equals(precedente.getRoleProposant().getCode()) || precedente.getReponse() != null) {
            throw new IllegalStateException("La proposition doit provenir du RH et etre sans reponse");
        }
        ConditionTravail proposition = nouvelleProposition(donnees, "CANDIDAT",
                Math.addExact(precedente.getVersion(), 1));
        proposition.setIdCandidature(precedente.getIdCandidature());
        precedente.setReponse(reponseRepository.findByCode("AUTRE")
                .orElseThrow(() -> new IllegalStateException("Reponse AUTRE introuvable")));
        conditionTravailRepository.save(precedente);
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> dossiers(Integer candidatId) {
        List<Map<String, Object>> dossiers = candidatureRepository.findDossiersConditions(candidatId);
        for (Map<String, Object> dossier : dossiers) {
            List<ConditionTravail> historique = conditionTravailRepository.findByIdCandidatureOrderByVersionDesc((Integer) dossier.get("id"));
            ConditionTravail modele = conditionTravailRepository.findFirstByPoste_IdOrderByVersionDesc((Integer) dossier.get("posteid")).orElse(null);
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
            actuelle = conditionTravailRepository.saveAndFlush(actuelle);
        }
        return actuelle;
    }

    @Transactional
    public void decider(Integer conditionId, boolean valider) {
        ConditionTravail condition = conditionTravailRepository.findByIdForUpdate(conditionId)
                .orElseThrow(() -> new IllegalArgumentException("Proposition introuvable"));
        if (condition.getIdCandidature() == null || !"CANDIDAT".equals(condition.getRoleProposant().getCode())
                || !"EN_ATTENTE".equals(condition.getDecisionRh().getCode())) {
            throw new IllegalStateException("Cette proposition n'est pas en attente de validation RH");
        }
        ConditionTravail derniere = conditionTravailRepository.findFirstByIdCandidatureOrderByVersionDesc(condition.getIdCandidature()).orElseThrow();
        if (!derniere.getId().equals(conditionId)) {
            throw new IllegalStateException("Cette proposition a ete remplacee");
        }
        condition.setDecisionRh(decisionRepository.findByCode(valider ? "VALIDEE" : "REFUSEE").orElseThrow());
        conditionTravailRepository.save(condition);
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
}
