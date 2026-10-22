
package com.project.dto;

import com.project.entity.Candidature;

/** Vue d'une candidature a traiter par le RH : etape en cours et reponses. */
public class CandidatureRhVue {

    private final Candidature candidature;
    private final QuestionnaireVue questionnaire;

    public CandidatureRhVue(Candidature candidature, QuestionnaireVue questionnaire) {
        this.candidature = candidature;
        this.questionnaire = questionnaire == null ? QuestionnaireVue.vide()
                : questionnaire;
    }

    public Candidature getCandidature() {
        return candidature;
    }

    public QuestionnaireVue getQuestionnaire() {
        return questionnaire;
    }

    public String getOffre() {
        return candidature.getOffre() != null
                && candidature.getOffre().getPoste() != null
                        ? candidature.getOffre().getPoste().getIntitule()
                        : "Poste inconnu";
    }

    public String getDepartement() {
        return candidature.getOffre() != null
                && candidature.getOffre().getDepartement() != null
                        ? candidature.getOffre().getDepartement().getNom()
                        : "-";
    }

    public String getCandidat() {
        return candidature.getCandidat() != null ? candidature.getCandidat().getEmail()
                : "-";
    }

    public String getEtape() {
        return candidature.getEtape() != null ? candidature.getEtape().getLibelle() : "-";
    }

    public String getCodeEtape() {
        return candidature.getEtape() != null ? candidature.getEtape().getCode() : "";
    }

    public String getStatut() {
        return candidature.getStatut() != null ? candidature.getStatut().getLibelle() : "-";
    }

    /** Libelle de l'etape vers laquelle la candidature sera transferee. */
    public String getEtapeSuivante() {
        if (candidature.getEtape() == null) {
            return "-";
        }
        Integer ordre = candidature.getEtape().getOrdre();
        return etapeSuivantes.getOrDefault(ordre, "Aucune (processus termine)");
    }

    private static final java.util.Map<Integer, String> etapeSuivantes =
            java.util.Map.of(1, "Questionnaire", 2, "Entretien", 3, "Conditions de travail",
                    4, "Embauche");

    /** Le questionnaire doit-il etre montre au RH (reponses a corriger) ? */
    public boolean isAfficheQuestionnaire() {
        return "QUESTIONNAIRE".equals(getCodeEtape()) && questionnaire.isRepondu();
    }
}
