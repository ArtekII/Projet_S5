
package com.project.dto;

import java.util.ArrayList;
import java.util.List;

import com.project.entity.Question;
import com.project.entity.Questionnaire;
import com.project.entity.ReponseCandidat;

/**
 * Questionnaire de l'etape "questionnaire" pour une candidature : les
 * questions a poser et les reponses deja donnees par le candidat.
 */
public class QuestionnaireVue {

    private final Questionnaire questionnaire;
    private final List<Question> questions;
    private final List<ReponseCandidat> reponses;

    private QuestionnaireVue(Questionnaire questionnaire, List<Question> questions,
            List<ReponseCandidat> reponses) {
        this.questionnaire = questionnaire;
        this.questions = questions;
        this.reponses = reponses;
    }

    /** Questionnaire disponible pour l'offre de la candidature. */
    public static QuestionnaireVue de(Questionnaire questionnaire, List<Question> questions,
            List<ReponseCandidat> reponses) {
        return new QuestionnaireVue(questionnaire, questions == null ? new ArrayList<>()
                : questions, reponses == null ? new ArrayList<>() : reponses);
    }

    /** Aucun questionnaire pour cette offre. */
    public static QuestionnaireVue vide() {
        return new QuestionnaireVue(null, new ArrayList<>(), new ArrayList<>());
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public List<ReponseCandidat> getReponses() {
        return reponses;
    }

    public String getTitre() {
        return questionnaire != null ? questionnaire.getTitre() : "";
    }

    /** Vrai si le questionnaire existe et contient des questions. */
    public boolean isDisponible() {
        return questionnaire != null && !questions.isEmpty();
    }

    /** Vrai si le candidat a deja envoye ses reponses. */
    public boolean isRepondu() {
        return !reponses.isEmpty();
    }

    /** Reponse du candidat a une question (null si pas encore repondu). */
    public ReponseCandidat getReponseDe(Question question) {
        for (ReponseCandidat reponse : reponses) {
            if (reponse.getQuestion() != null
                    && reponse.getQuestion().getId().equals(question.getId())) {
                return reponse;
            }
        }
        return null;
    }
}
