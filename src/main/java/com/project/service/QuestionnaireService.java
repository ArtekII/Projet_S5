
package com.project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.Candidature;
import com.project.entity.ChoixReponse;
import com.project.entity.Question;
import com.project.entity.Questionnaire;
import com.project.entity.ReponseCandidat;
import com.project.repository.CandidatureRepository;
import com.project.repository.ChoixReponseRepository;
import com.project.repository.QuestionRepository;
import com.project.repository.QuestionnaireRepository;
import com.project.repository.ReponseCandidatRepository;

@Service
@Transactional(readOnly = true)
public class QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ChoixReponseRepository choixReponseRepository;
    private final ReponseCandidatRepository reponseCandidatRepository;
    private final CandidatureRepository candidatureRepository;

    public QuestionnaireService(
            QuestionnaireRepository questionnaireRepository,
            QuestionRepository questionRepository,
            ChoixReponseRepository choixReponseRepository,
            ReponseCandidatRepository reponseCandidatRepository,
            CandidatureRepository candidatureRepository) {
        this.questionnaireRepository = questionnaireRepository;
        this.questionRepository = questionRepository;
        this.choixReponseRepository = choixReponseRepository;
        this.reponseCandidatRepository = reponseCandidatRepository;
        this.candidatureRepository = candidatureRepository;
    }

    /** Questionnaire de l'etape questionnaire pour une offre donnee. */
    public Optional<Questionnaire> getQuestionnaire(Integer idOffre) {
        return questionnaireRepository.findByOffre_Id(idOffre);
    }

    /** Questions d'un questionnaire, avec leurs choix. */
    public List<Question> getQuestions(Integer idQuestionnaire) {
        return questionRepository.findByQuestionnaireIdAvecChoix(idQuestionnaire);
    }

    /** Reponses deja donnees par le candidat. */
    public List<ReponseCandidat> getReponses(Integer idCandidature) {
        return reponseCandidatRepository.findByCandidature_Id(idCandidature);
    }

    /**
     * Enregistre les reponses du candidat au questionnaire et calcule le score
     * des QCM (les reponses libres sont notees par le RH).
     *
     * @param choixParQuestion identifiant de question -&gt; choix selectionne
     * @param texteParQuestion identifiant de question -&gt; reponse libre
     * @return score obtenu sur les QCM
     */
    @Transactional
    public BigDecimal enregistrerReponses(Candidature candidature,
            List<Question> questions,
            Map<Integer, Integer> choixParQuestion,
            Map<Integer, String> texteParQuestion) {

        BigDecimal score = BigDecimal.ZERO;

        for (Question question : questions) {
            Integer idQuestion = question.getId();
            Integer idChoix = choixParQuestion.get(idQuestion);
            String texte = texteParQuestion.get(idQuestion);

            if (idChoix == null && (texte == null || texte.isBlank())) {
                continue;
            }

            ReponseCandidat reponse = reponseCandidatRepository
                    .findByCandidature_IdAndQuestion_Id(candidature.getId(), idQuestion)
                    .orElseGet(ReponseCandidat::new);
            reponse.setCandidature(candidature);
            reponse.setQuestion(question);
            reponse.setChoix(null);
            reponse.setReponseTexte(null);
            reponse.setPointsObtenus(null);

            if (idChoix != null) {
                ChoixReponse choix = choixReponseRepository.findById(idChoix)
                        .orElseThrow(() -> new IllegalStateException(
                                "Choix de reponse introuvable : " + idChoix));
                reponse.setChoix(choix);
                if (choix.isEstCorrect()) {
                    reponse.setPointsObtenus(question.getPoints());
                    score = score.add(question.getPoints());
                } else {
                    reponse.setPointsObtenus(BigDecimal.ZERO);
                }
            } else {
                // reponse libre : la note est mise par le RH
                reponse.setReponseTexte(texte.trim());
            }

            reponseCandidatRepository.save(reponse);
        }

        candidature.setScoreQuestionnaire(score);
        candidatureRepository.save(candidature);
        return score;
    }
}
