
package com.project.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.entity.Candidature;
import com.project.entity.Question;
import com.project.entity.Questionnaire;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.CandidatureService;
import com.project.service.QuestionnaireService;

import jakarta.servlet.http.HttpSession;

/** Actions du candidat : depot du CV (postulation) et reponse au questionnaire. */
@Controller
@RequestMapping("/candidatures")
public class CandidatureController {

    private final AuthService authService;
    private final CandidatureService candidatureService;
    private final QuestionnaireService questionnaireService;

    public CandidatureController(AuthService authService,
            CandidatureService candidatureService,
            QuestionnaireService questionnaireService) {
        this.authService = authService;
        this.candidatureService = candidatureService;
        this.questionnaireService = questionnaireService;
    }

    /** Depot du CV : cree la candidature a l'etape 1 (en attente de validation RH). */
    @PostMapping("/cv")
    public String deposerCv(@RequestParam("id_offre") Integer idOffre,
            @RequestParam(value = "nom", required = false) String nom,
            @RequestParam(value = "prenom", required = false) String prenom,
            @RequestParam(value = "date_naissance", required = false) String dateNaissance,
            @RequestParam(value = "annee_experience", required = false) Integer anneeExperience,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "diplome", required = false) String diplome,
            @RequestParam(value = "competences", required = false) String competences,
            HttpSession session, RedirectAttributes redirect) {

        Utilisateur candidat = authService.currentUser(session).orElse(null);
        if (candidat == null) {
            return "redirect:/login";
        }

        try {
            candidatureService.postuler(candidat.getId(), idOffre,
                    nom != null && !nom.isBlank() ? nom : "Candidat",
                    prenom != null && !prenom.isBlank() ? prenom : "Candidat",
                    parseDate(dateNaissance),
                    anneeExperience,
                    email != null && !email.isBlank() ? email : candidat.getEmail(),
                    diplome, competences);
            redirect.addFlashAttribute("success",
                    "Postulation enregistrée : votre candidature est en attente de validation par le RH.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/espaceCandidat?onglet=cand";
    }

    /** Envoi des reponses au questionnaire de l'etape en cours. */
    @PostMapping("/questionnaire")
    public String envoyerQuestionnaire(
            @RequestParam("id_candidature") Integer idCandidature,
            @RequestParam Map<String, String> params,
            HttpSession session, RedirectAttributes redirect) {

        Utilisateur candidat = authService.currentUser(session).orElse(null);
        if (candidat == null) {
            return "redirect:/login";
        }

        Candidature candidature = candidatureService.findCandidature(idCandidature).orElse(null);
        if (candidature == null) {
            redirect.addFlashAttribute("error", "Candidature introuvable.");
            return "redirect:/espaceCandidat?onglet=cand";
        }
        if (candidature.getCandidat() == null
                || !candidature.getCandidat().getId().equals(candidat.getId())) {
            redirect.addFlashAttribute("error", "Cette candidature ne vous appartient pas.");
            return "redirect:/espaceCandidat?onglet=cand";
        }

        Questionnaire questionnaire = questionnaireService
                .getQuestionnaire(candidature.getOffre().getId()).orElse(null);
        if (questionnaire == null) {
            redirect.addFlashAttribute("error",
                    "Aucun questionnaire n'est associé à cette offre.");
            return "redirect:/espaceCandidat?onglet=cand";
        }

        List<Question> questions = questionnaireService.getQuestions(questionnaire.getId());
        Map<Integer, Integer> choixParQuestion = new HashMap<>();
        Map<Integer, String> texteParQuestion = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                if (key.startsWith("choix_")) {
                    choixParQuestion.put(Integer.parseInt(key.substring(6)),
                            Integer.parseInt(value));
                } else if (key.startsWith("texte_")) {
                    texteParQuestion.put(Integer.parseInt(key.substring(6)), value);
                }
            } catch (NumberFormatException ignored) {
                // parametre ignore
            }
        }

        try {
            questionnaireService.enregistrerReponses(candidature, questions, choixParQuestion,
                    texteParQuestion);
            redirect.addFlashAttribute("success",
                    "Vos réponses ont été enregistrées : elles sont en attente de validation par le RH.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/espaceCandidat?onglet=cand";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
