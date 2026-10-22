
package com.project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.Candidature;
import com.project.entity.ChoixReponse;
import com.project.entity.Question;
import com.project.entity.Questionnaire;
import com.project.entity.ReponseCandidat;
import com.project.entity.OffreEmploi;
import com.project.entity.TypeQuestion;
import com.project.entity.Utilisateur;
import com.project.repository.CandidatureRepository;
import com.project.repository.ChoixReponseRepository;
import com.project.repository.QuestionRepository;
import com.project.repository.QuestionnaireRepository;
import com.project.repository.ReponseCandidatRepository;
import com.project.repository.TypeQuestionRepository;
import com.project.repository.OffreEmploiRepository;

@Service
@Transactional(readOnly = true)
public class QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ChoixReponseRepository choixReponseRepository;
    private final ReponseCandidatRepository reponseCandidatRepository;
    private final CandidatureRepository candidatureRepository;
    private final TypeQuestionRepository typeQuestionRepository;
    private final OffreEmploiRepository offreRepository;

    public QuestionnaireService(
            QuestionnaireRepository questionnaireRepository,
            QuestionRepository questionRepository,
            ChoixReponseRepository choixReponseRepository,
            ReponseCandidatRepository reponseCandidatRepository,
            CandidatureRepository candidatureRepository,
            TypeQuestionRepository typeQuestionRepository,
            OffreEmploiRepository offreRepository) {
        this.questionnaireRepository = questionnaireRepository;
        this.questionRepository = questionRepository;
        this.choixReponseRepository = choixReponseRepository;
        this.reponseCandidatRepository = reponseCandidatRepository;
        this.candidatureRepository = candidatureRepository;
        this.typeQuestionRepository = typeQuestionRepository;
        this.offreRepository = offreRepository;
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

    public List<Questionnaire> getQuestionnaires() {
        return questionnaireRepository.findAllAvecOffre();
    }

    /**
     * Cree le questionnaire d'une offre. Chaque offre ne peut avoir qu'un
     * questionnaire actif afin que les candidatures utilisent toujours la
     * meme grille de notation.
     */
    @Transactional
    public Questionnaire creerQuestionnaire(Integer idOffre, Utilisateur createur,
            String titre, BigDecimal seuil, List<String> enonces, List<BigDecimal> points,
            List<String> types, List<String> choix, List<Integer> bonnesReponses) {
        OffreEmploi offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new IllegalStateException("Offre introuvable."));
        if (questionnaireRepository.findByOffre_Id(idOffre).isPresent()) {
            throw new IllegalStateException("Cette offre possède déjà un questionnaire.");
        }
        if (titre == null || titre.isBlank() || enonces == null || enonces.isEmpty()) {
            throw new IllegalArgumentException("Le titre et au moins une question sont obligatoires.");
        }

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setOffre(offre);
        questionnaire.setCreateur(createur);
        questionnaire.setTitre(titre.trim());
        questionnaire.setSeuilReussite(seuil == null ? BigDecimal.ZERO : seuil);
        Questionnaire saved = questionnaireRepository.save(questionnaire);

        int ordre = 1;
        for (int i = 0; i < enonces.size(); i++) {
            String enonce = enonces.get(i);
            if (enonce == null || enonce.isBlank()) {
                continue;
            }
            Question question = new Question();
            question.setQuestionnaire(saved);
            question.setEnonce(enonce.trim());
            question.setOrdre(ordre++);
            question.setPoints(points != null && i < points.size() && points.get(i) != null
                    ? points.get(i) : BigDecimal.ONE);
            String codeType = types != null && i < types.size() ? types.get(i) : "TEXTE";
            TypeQuestion type = typeQuestionRepository.findByCode(codeType)
                    .orElseThrow(() -> new IllegalArgumentException("Type de question inconnu."));
            question.setType(type);
            Question savedQuestion = questionRepository.save(question);

            if ("QCM".equals(codeType) && choix != null && i < choix.size()) {
                String[] propositions = choix.get(i).split("\\|", -1);
                int bonneReponse = bonnesReponses != null && i < bonnesReponses.size()
                        && bonnesReponses.get(i) != null ? bonnesReponses.get(i) : -1;
                for (int j = 0; j < propositions.length; j++) {
                    if (propositions[j].isBlank()) {
                        continue;
                    }
                    ChoixReponse choixReponse = new ChoixReponse();
                    choixReponse.setQuestion(savedQuestion);
                    choixReponse.setLibelle(propositions[j].trim());
                    choixReponse.setEstCorrect(j == bonneReponse);
                    choixReponseRepository.save(choixReponse);
                }
            }
        }
        if (ordre == 1) {
            throw new IllegalArgumentException("Ajoutez au moins une question non vide.");
        }
        return saved;
    }

    /** Modifie le questionnaire existant d'une offre sans supprimer les reponses deja envoyees. */
    @Transactional
    public Questionnaire modifierQuestionnaire(Integer idOffre, String titre, BigDecimal seuil,
            List<String> enonces, List<BigDecimal> points, List<String> types,
            List<String> choix, List<Integer> bonnesReponses) {
        Questionnaire questionnaire = questionnaireRepository.findByOffre_Id(idOffre)
                .orElseThrow(() -> new IllegalStateException("Questionnaire introuvable."));
        if (titre == null || titre.isBlank()) {
            throw new IllegalArgumentException("Le titre du questionnaire est obligatoire.");
        }
        questionnaire.setTitre(titre.trim());
        questionnaire.setSeuilReussite(seuil == null ? BigDecimal.ZERO : seuil);
        Questionnaire saved = questionnaireRepository.save(questionnaire);

        List<Question> questionsExistantes = questionRepository
                .findByQuestionnaireIdAvecChoix(saved.getId());
        for (int i = 0; i < enonces.size() && i < questionsExistantes.size(); i++) {
            if (enonces.get(i) == null || enonces.get(i).isBlank()) {
                continue;
            }
            Question question = questionsExistantes.get(i);
            question.setEnonce(enonces.get(i).trim());
            if (points != null && i < points.size() && points.get(i) != null) {
                question.setPoints(points.get(i));
            }
            String codeType = types != null && i < types.size() ? types.get(i) : "TEXTE";
            TypeQuestion type = typeQuestionRepository.findByCode(codeType)
                    .orElseThrow(() -> new IllegalArgumentException("Type de question inconnu."));
            question.setType(type);
            Question savedQuestion = questionRepository.save(question);

            if ("QCM".equals(codeType) && choix != null && i < choix.size()) {
                String[] propositions = choix.get(i).split("\\|", -1);
                int bonneReponse = bonnesReponses != null && i < bonnesReponses.size()
                        && bonnesReponses.get(i) != null ? bonnesReponses.get(i) : -1;
                List<ChoixReponse> choixExistants = savedQuestion.getChoix();
                for (int j = 0; j < propositions.length; j++) {
                    if (propositions[j].isBlank()) {
                        continue;
                    }
                    ChoixReponse choixReponse;
                    if (j < choixExistants.size()) {
                        choixReponse = choixExistants.get(j);
                    } else {
                        choixReponse = new ChoixReponse();
                        choixReponse.setQuestion(savedQuestion);
                        choixExistants.add(choixReponse);
                    }
                    choixReponse.setLibelle(propositions[j].trim());
                    choixReponse.setEstCorrect(j == bonneReponse);
                    choixReponseRepository.save(choixReponse);
                }
            }
        }
        recalculerScoresApresModification(idOffre);
        return saved;
    }

    /**
     * Recalcule les points déjà attribués après une modification du barème ou
     * de la bonne réponse d'un QCM.
     */
    private void recalculerScoresApresModification(Integer idOffre) {
        Map<Integer, BigDecimal> scores = new HashMap<>();
        for (ReponseCandidat reponse : reponseCandidatRepository
                .findByCandidature_Offre_Id(idOffre)) {
            BigDecimal points = reponse.getPointsObtenus();
            if (reponse.getChoix() != null) {
                points = reponse.getChoix().isEstCorrect()
                        ? reponse.getQuestion().getPoints() : BigDecimal.ZERO;
                reponse.setPointsObtenus(points);
                reponseCandidatRepository.save(reponse);
            }
            if (points != null) {
                Integer idCandidature = reponse.getCandidature().getId();
                scores.merge(idCandidature, points, BigDecimal::add);
            }
        }
        for (Candidature candidature : candidatureRepository.findByOffreIdAvecDetails(idOffre)) {
            candidature.setScoreQuestionnaire(
                    scores.getOrDefault(candidature.getId(), BigDecimal.ZERO));
            candidatureRepository.save(candidature);
        }
    }

    /** Note les réponses libres et recalcule le score total de la candidature. */
    @Transactional
    public BigDecimal noterReponses(Integer idCandidature, Map<Integer, BigDecimal> notes) {
        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new IllegalStateException("Candidature introuvable."));
        if (candidature.getEtape() == null
                || !"QUESTIONNAIRE".equals(candidature.getEtape().getCode())) {
            throw new IllegalStateException("Cette candidature n'est pas à l'étape questionnaire.");
        }
        for (ReponseCandidat reponse : getReponses(idCandidature)) {
            BigDecimal note = notes.get(reponse.getQuestion().getId());
            if (note != null) {
                BigDecimal maximum = reponse.getQuestion().getPoints();
                if (note.signum() < 0 || note.compareTo(maximum) > 0) {
                    throw new IllegalArgumentException("Une note doit être comprise entre 0 et "
                            + maximum + ".");
                }
                reponse.setPointsObtenus(note);
                reponseCandidatRepository.save(reponse);
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ReponseCandidat reponse : getReponses(idCandidature)) {
            if (reponse.getPointsObtenus() != null) {
                total = total.add(reponse.getPointsObtenus());
            }
        }
        candidature.setScoreQuestionnaire(total);
        candidatureRepository.save(candidature);
        return total;
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

        if (candidature.getEtape() == null
                || !"QUESTIONNAIRE".equals(candidature.getEtape().getCode())) {
            throw new IllegalStateException(
                    "Le questionnaire sera disponible après validation RH de votre candidature.");
        }

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
