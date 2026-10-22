
package com.project.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.project.entity.Candidature;
import com.project.entity.CvCandidat;
import com.project.entity.OffreEmploi;

/**
 * Vue d'une candidature du candidat connecte : l'offre postulee, le CV depose
 * et les etapes deja franchies (table {@code candidats_selec}).
 */
public class CandidatureVue {

    private final OffreEmploi offre;
    private final List<EtapeVue> etapes = new ArrayList<>();
    private CvCandidat cvCandidat;
    private Candidature candidature;
    private QuestionnaireVue questionnaire = QuestionnaireVue.vide();
    private boolean etapeValidee;

    public CandidatureVue(OffreEmploi offre) {
        this.offre = offre;
    }

    // ------------------------------------------------------- etape courante

    /** Code de l'etape de recrutement en cours (CV, QUESTIONNAIRE, ...). */
    public String getCodeEtape() {
        return candidature != null && candidature.getEtape() != null
                ? candidature.getEtape().getCode()
                : "";
    }

    /** Libelle de l'etape de recrutement en cours. */
    public String getLibelleEtape() {
        return candidature != null && candidature.getEtape() != null
                ? candidature.getEtape().getLibelle()
                : "";
    }

    /** Rang de l'etape en cours dans le processus de recrutement. */
    public Integer getOrdreEtape() {
        return candidature != null && candidature.getEtape() != null
                ? candidature.getEtape().getOrdre()
                : 0;
    }

    /** Vrai si le RH a deja valide l'etape en cours. */
    public boolean isEtapeValidee() {
        return etapeValidee;
    }

    public void setEtapeValidee(boolean etapeValidee) {
        this.etapeValidee = etapeValidee;
    }

    /** Questionnaire de l'etape questionnaire pour l'offre de la candidature. */
    public QuestionnaireVue getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(QuestionnaireVue questionnaire) {
        this.questionnaire = questionnaire == null ? QuestionnaireVue.vide()
                : questionnaire;
    }

    /** Vrai si l'etape en cours est le questionnaire. */
    public boolean isEtapeQuestionnaire() {
        return "QUESTIONNAIRE".equals(getCodeEtape());
    }

    /** Vrai si la candidature est encore en cours de traitement. */
    public boolean isEnCours() {
        return candidature == null || candidature.getStatut() == null
                || "EN_COURS".equals(candidature.getStatut().getCode());
    }

    /** Vrai si le questionnaire doit etre affiche au candidat. */
    public boolean isQuestionnaireARemplir() {
        return isEnCours() && isEtapeQuestionnaire() && questionnaire.isDisponible()
                && !questionnaire.isRepondu();
    }

    // ---------------------------------------------------------------- offre

    public OffreEmploi getOffre() {
        return offre;
    }

    public String getIntitulePoste() {
        return offre != null && offre.getPoste() != null ? offre.getPoste().getIntitule()
                : "Poste inconnu";
    }

    public String getDepartement() {
        return offre != null && offre.getDepartement() != null ? offre.getDepartement().getNom()
                : "-";
    }

    // ---------------------------------------------------------- candidature

    public Candidature getCandidature() {
        return candidature;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public CvCandidat getCvCandidat() {
        return cvCandidat;
    }

    public void setCvCandidat(CvCandidat cvCandidat) {
        this.cvCandidat = cvCandidat;
    }

    /** Date de postulation : la candidature si elle existe, sinon le CV depose. */
    public LocalDateTime getDatePostulation() {
        if (candidature != null && candidature.getCreatedAt() != null) {
            return candidature.getCreatedAt();
        }
        return cvCandidat != null ? cvCandidat.getCreatedAt() : null;
    }

    /** Libelle du statut affiche (statut de la candidature ou etape en cours). */
    public String getStatutLibelle() {
        if (candidature != null && candidature.getStatut() != null) {
            String code = candidature.getStatut().getCode();
            if (!"EN_COURS".equals(code)) {
                return candidature.getStatut().getLibelle();
            }
        }
        if (candidature == null) {
            return etapes.isEmpty() ? "En attente de validation RH" : "En cours";
        }
        switch (getCodeEtape()) {
            case "CV":
                return etapeValidee ? "CV validé — étape suivante"
                        : "En attente de validation RH";
            case "QUESTIONNAIRE":
                if (questionnaire.isRepondu()) {
                    return "Réponses envoyées — en attente de validation RH";
                }
                return "Questionnaire à remplir";
            case "ENTRETIEN":
                return etapeValidee ? "Entretien validé" : "Entretien à venir";
            default:
                return getLibelleEtape() + " en cours";
        }
    }

    /** Classe CSS du badge de statut (st-run / st-wait / st-ko / st-ok). */
    public String getStatutClasse() {
        if (candidature != null && candidature.getStatut() != null) {
            switch (candidature.getStatut().getCode()) {
                case "EMBAUCHE":
                    return "st-ok";
                case "ELIMINE":
                case "REFUSE_PAR_CANDIDAT":
                    return "st-ko";
                default:
                    break;
            }
        }
        if (candidature == null) {
            return etapes.isEmpty() ? "st-wait" : "st-run";
        }
        if ("CV".equals(getCodeEtape()) && !etapeValidee) {
            return "st-wait";
        }
        if ("QUESTIONNAIRE".equals(getCodeEtape())) {
            return questionnaire.isRepondu() ? "st-wait" : "st-run";
        }
        return "st-run";
    }

    /** Nom de la derniere etape franchie (l'etape en cours). */
    public String getEtapeCourante() {
        return etapes.isEmpty() ? getLibelleEtape() : etapes.get(etapes.size() - 1).getNom();
    }

    /** Phrase resume l'etat des etapes de la candidature. */
    public String getResumeEtapes() {
        if (candidature != null && candidature.getStatut() != null) {
            String code = candidature.getStatut().getCode();
            if ("ELIMINE".equals(code) || "REFUSE_PAR_CANDIDAT".equals(code)) {
                return "Candidature arrêtée : " + candidature.getStatut().getLibelle()
                        + ".";
            }
        }
        if (candidature == null) {
            if (etapes.isEmpty()) {
                return "Votre candidature est en attente de validation par le RH.";
            }
            return "Étape en cours : " + getEtapeCourante() + " — " + etapes.size()
                    + " étape(s) franchie(s).";
        }
        switch (getCodeEtape()) {
            case "CV":
                return "Votre postulation a bien été enregistrée : elle est en attente de "
                        + "validation par le RH.";
            case "QUESTIONNAIRE":
                if (questionnaire.isRepondu()) {
                    return "Vos réponses au questionnaire ont été envoyées : elles sont en "
                            + "attente de validation par le RH.";
                }
                return "Étape en cours : questionnaire — répondez aux questions ci-dessous.";
            default:
                return "Étape en cours : " + getLibelleEtape() + " — " + etapes.size()
                        + " étape(s) franchie(s).";
        }
    }

    /** Nombre d'etapes franchies. */
    public int getNbEtapes() {
        return etapes.size();
    }

    /** Vrai si le candidat a deja postule a cette offre. */
    public boolean isPostule() {
        return candidature != null || !etapes.isEmpty();
    }

    /** Les etapes franchies, dans l'ordre chronologique. */
    public List<EtapeVue> getEtapes() {
        return etapes;
    }
}
