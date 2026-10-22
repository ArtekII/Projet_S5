package com.project.controller;

import com.project.dto.CandidatureRhVue;
import com.project.dto.QuestionnaireVue;
import com.project.entity.Candidature;
import com.project.entity.Question;
import com.project.entity.Questionnaire;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.CandidatureService;
import com.project.service.OffreService;
import com.project.service.QuestionnaireService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/rh")
public class RhController {
  private final OffreService offreService;
  private final AuthService authService;
  private final CandidatureService candidatureService;
  private final QuestionnaireService questionnaireService;

  public RhController(OffreService o, AuthService a, CandidatureService c,
      QuestionnaireService q) {
    this.offreService = o;
    this.authService = a;
    this.candidatureService = c;
    this.questionnaireService = q;
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("postes", offreService.postesActifs());
    model.addAttribute("offresAttente", offreService.offresEnAttenteRh());
    model.addAttribute("demandesAttente", offreService.demandesEnAttente());
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    model.addAttribute("toutesDemandes", offreService.toutesDemandes());
    model.addAttribute("candidaturesRh", buildCandidaturesRh());
    return "rh/dashboard";
  }

  /** Vue RH des candidatures en cours : etape en cours et reponses au questionnaire. */
  private List<CandidatureRhVue> buildCandidaturesRh() {
    List<CandidatureRhVue> vues = new ArrayList<>();
    for (Candidature candidature : candidatureService.getCandidaturesEnCours()) {
      QuestionnaireVue questionnaire = QuestionnaireVue.vide();
      if (candidature.getEtape() != null
          && "QUESTIONNAIRE".equals(candidature.getEtape().getCode())) {
        Questionnaire q = questionnaireService
            .getQuestionnaire(candidature.getOffre().getId()).orElse(null);
        if (q != null) {
          List<Question> questions = questionnaireService.getQuestions(q.getId());
          questionnaire = QuestionnaireVue.de(q, questions,
              questionnaireService.getReponses(candidature.getId()));
        }
      }
      vues.add(new CandidatureRhVue(candidature, questionnaire));
    }
    return vues;
  }

  /** Le RH valide l'etape en cours : la candidature passe a l'etape suivante. */
  @PostMapping("/candidature/{id}/valider")
  public String validerCandidature(@PathVariable Integer id, HttpSession session,
      RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      candidatureService.validerEtapeSuivante(id, u);
      ra.addFlashAttribute("success", "Etape validee : la candidature passe a l'etape suivante.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  /** Le RH elimine la candidature. */
  @PostMapping("/candidature/{id}/eliminer")
  public String eliminerCandidature(@PathVariable Integer id, HttpSession session,
      RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      candidatureService.eliminer(id, u);
      ra.addFlashAttribute("success", "Candidature eliminee.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/offre/{id}/valider")
  public String validerOffre(@PathVariable Integer id,
      @RequestParam(required = false) Integer posteId,
      @RequestParam(required = false) Integer anneesExperience,
      @RequestParam(required = false) Integer nbPlaces,
      @RequestParam BigDecimal salaire,
      @RequestParam(required = false) String commentaire,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.rhValiderOffre(id, posteId, anneesExperience, nbPlaces, salaire, u, true, commentaire);
      ra.addFlashAttribute("success", "Offre validee -> transmise a la Finance.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/offre/{id}/refuser")
  public String refuserOffre(@PathVariable Integer id, @RequestParam(required = false) String commentaire,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.rhValiderOffre(id, null, null, null, null, u, false, commentaire);
      ra.addFlashAttribute("success", "Offre refusee par RH.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/demande/{id}/valider")
  public String validerDemande(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    offreService.traiterDemandePoste(id, true, null, u);
    ra.addFlashAttribute("success", "Poste ajoute au catalogue (dropdown manager).");
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/demande/{id}/refuser")
  public String refuserDemande(@PathVariable Integer id, @RequestParam(required = false) String motif,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    offreService.traiterDemandePoste(id, false, motif, u);
    ra.addFlashAttribute("success", "Demande de poste refusee.");
    return "redirect:/rh/dashboard";
  }
}
