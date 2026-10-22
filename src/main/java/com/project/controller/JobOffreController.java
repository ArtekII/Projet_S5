
package com.project.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.dto.CandidatureVue;
import com.project.dto.EtapeVue;
import com.project.dto.QuestionnaireVue;
import com.project.entity.CandidatsSelec;
import com.project.entity.Candidature;
import com.project.entity.CandidatureEtape;
import com.project.entity.Etape;
import com.project.entity.OffreEmploi;
import com.project.entity.Question;
import com.project.entity.Questionnaire;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.CandidatsSelecService;
import com.project.service.CandidatureService;
import com.project.service.EtapeRecrutementService;
import com.project.service.EtapeService;
import com.project.service.OffreService;
import com.project.service.QuestionnaireService;

import jakarta.servlet.http.HttpSession;

@Controller
public class JobOffreController {

    private final OffreService offreService;
    private final CandidatureService candidatureService;
    private final AuthService authService;
    private final CandidatsSelecService candidatsSelecService;
    private final EtapeService etapeService;
    private final EtapeRecrutementService etapeRecrutementService;
    private final QuestionnaireService questionnaireService;

    public JobOffreController(
            OffreService offreService,
            CandidatureService candidatureService,
            AuthService authService,
            CandidatsSelecService candidatsSelecService,
            EtapeService etapeService,
            EtapeRecrutementService etapeRecrutementService,
            QuestionnaireService questionnaireService) {
        this.offreService = offreService;
        this.candidatureService = candidatureService;
        this.authService = authService;
        this.candidatsSelecService = candidatsSelecService;
        this.etapeService = etapeService;
        this.etapeRecrutementService = etapeRecrutementService;
        this.questionnaireService = questionnaireService;
    }

    @GetMapping("/jobOffre")
    public String getJobOffre(Model model) {
        model.addAttribute("offres", offreService.listerOffresPubliees());
        return "portail/listJob";
    }

    /**
     * Espace du candidat connecte : les offres disponibles, ses candidatures et
     * les etapes de candidature deja franchies (table {@code candidats_selec}).
     */
    @GetMapping("/espaceCandidat")
    public String getEspaceCandidat(Model model, HttpSession session,
            @RequestParam(name = "offre", required = false) Integer offreId,
            @RequestParam(name = "onglet", required = false) String onglet) {

        Utilisateur utilisateur = authService.currentUser(session).orElse(null);

        // Rediriger si l'utilisateur n'est pas connecté
        if (utilisateur == null) {
            return "redirect:/login";
        }

        List<OffreEmploi> offres = offreService.listerOffresPubliees();
        List<Candidature> candidatures = candidatureService
                .getCandidaturesAvecDetails(utilisateur.getId());
        Map<Integer, List<CandidatsSelec>> etapesParOffre = candidatsSelecService
                .getSelectionsParOffre(utilisateur.getId());
        List<Etape> etapes = etapeService.getEtapes();

        List<CandidatureVue> candidatureVues = buildCandidatureVues(candidatures,
                etapesParOffre);

        model.addAttribute("user", utilisateur);
        model.addAttribute("userEmail", utilisateur.getEmail());
        model.addAttribute("userId", utilisateur.getId());
        model.addAttribute("offres", offres);
        model.addAttribute("candidatures", candidatures);
        model.addAttribute("etapesParOffre", etapesParOffre);
        model.addAttribute("candidatureVues", candidatureVues);
        model.addAttribute("etapes", etapes);
        model.addAttribute("etapesRecrutement", etapeRecrutementService.getEtapes());
        model.addAttribute("nbOffres", offres.size());
        model.addAttribute("nbCandidatures", candidatureVues.size());
        model.addAttribute("offresPostulees", offresPostulees(candidatureVues));

        // Onglet affiche : le depot de CV est ouvert si une offre est preselectionnee
        model.addAttribute("onglet", onglet != null ? onglet
                : (offreId != null ? "cv" : "offres"));
        model.addAttribute("offreSelectionnee", offreId);

        return "candidat/espaceCandidat";
    }

    /**
     * Regroupe par offre les candidatures du candidat et ses etapes
     * ({@code candidats_selec}) : une ligne par offre postulee.
     */
    private List<CandidatureVue> buildCandidatureVues(List<Candidature> candidatures,
            Map<Integer, List<CandidatsSelec>> etapesParOffre) {

        Map<Integer, CandidatureVue> vues = new LinkedHashMap<>();

        for (Candidature candidature : candidatures) {
            Integer idOffre = candidature.getOffre().getId();
            CandidatureVue vue = vues.computeIfAbsent(idOffre,
                    id -> new CandidatureVue(candidature.getOffre()));
            vue.setCandidature(candidature);
            vue.setEtapeValidee(etapeValidee(candidature));
            vue.setQuestionnaire(chargerQuestionnaire(candidature));
        }

        for (Map.Entry<Integer, List<CandidatsSelec>> entry : etapesParOffre.entrySet()) {
            for (CandidatsSelec selection : entry.getValue()) {
                Integer idOffre = selection.getOffreEmploi().getId();
                CandidatureVue vue = vues.computeIfAbsent(idOffre,
                        id -> new CandidatureVue(selection.getOffreEmploi()));
                if (vue.getCvCandidat() == null) {
                    vue.setCvCandidat(selection.getCvCandidat());
                }
                vue.getEtapes().add(EtapeVue.from(selection));
            }
        }

        List<CandidatureVue> liste = new ArrayList<>(vues.values());
        liste.sort(Comparator.comparing(CandidatureVue::getDatePostulation,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return liste;
    }

    /** Vrai si le RH a deja valide l'etape en cours de la candidature. */
    private boolean etapeValidee(Candidature candidature) {
        if (candidature.getEtape() == null) {
            return false;
        }
        for (CandidatureEtape etape : candidatureService
                .getEtapesFranchies(candidature.getId())) {
            if (etape.getEtape() != null && etape.getEtape().getId()
                    .equals(candidature.getEtape().getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Questionnaire de l'etape en cours : charge les questions et les reponses
     * deja donnees par le candidat.
     */
    private QuestionnaireVue chargerQuestionnaire(Candidature candidature) {
        if (candidature.getEtape() == null
                || !"QUESTIONNAIRE".equals(candidature.getEtape().getCode())) {
            return QuestionnaireVue.vide();
        }
        Questionnaire questionnaire = questionnaireService
                .getQuestionnaire(candidature.getOffre().getId()).orElse(null);
        if (questionnaire == null) {
            return QuestionnaireVue.vide();
        }
        List<Question> questions = questionnaireService.getQuestions(questionnaire.getId());
        return QuestionnaireVue.de(questionnaire, questions,
                questionnaireService.getReponses(candidature.getId()));
    }

    /** Identifiants des offres auxuelles le candidat a deja postule. */
    private Set<Integer> offresPostulees(List<CandidatureVue> candidatureVues) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (CandidatureVue vue : candidatureVues) {
            if (vue.isPostule() && vue.getOffre() != null) {
                ids.add(vue.getOffre().getId());
            }
        }
        return ids;
    }
}
