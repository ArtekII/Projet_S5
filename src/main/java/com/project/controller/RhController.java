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
import com.project.service.RecrutementService;
import com.project.entity.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/rh")
public class RhController {
  private final OffreService offreService;
  private final AuthService authService;
  private final RecrutementService recrutementService;

  public RhController(OffreService o, AuthService a, RecrutementService r) {
    this.offreService = o;
    this.authService = a;
    this.recrutementService = r;
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

  @GetMapping("/offre/{id}/recrutement")
  public String recrutement(@PathVariable Integer id, HttpSession session, Model model) {
    authService.currentUser(session).orElseThrow();
    model.addAttribute("offre", offreService.findOffre(id).orElseThrow());
    model.addAttribute("candidatures", recrutementService.candidatures(id));
    model.addAttribute("selection1", recrutementService.selections1(id));
    model.addAttribute("questions", recrutementService.questions(id));
    model.addAttribute("top5", recrutementService.top5(id));
    model.addAttribute("entretiens", recrutementService.entretiens(id));
    model.addAttribute("classement", recrutementService.classement(id));
    return "rh/recrutement";
  }

  @PostMapping("/offre/{id}/selection-cv")
  public String selectionCv(@PathVariable Integer id, @RequestParam(required = false) List<Integer> cvIds,
      HttpSession session, RedirectAttributes ra) {
    authService.currentUser(session).orElseThrow();
    try {
      recrutementService.validerSelection1(id, cvIds);
      ra.addFlashAttribute("success", "Première sélection enregistrée.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/offre/" + id + "/recrutement";
  }

  @PostMapping("/offre/{id}/question")
  public String question(@PathVariable Integer id, @RequestParam String texte, @RequestParam Integer pointsMax,
      HttpSession session, RedirectAttributes ra) {
    authService.currentUser(session).orElseThrow();
    try {
      recrutementService.ajouterQuestion(id, texte, pointsMax);
      ra.addFlashAttribute("success", "Question ajoutée.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/offre/" + id + "/recrutement";
  }

  @PostMapping("/offre/{id}/notes-questionnaire")
  public String notesQuestionnaire(@PathVariable Integer id, @RequestParam Map<String, String> params,
      HttpSession session, RedirectAttributes ra) {
    authService.currentUser(session).orElseThrow();
    try {
      recrutementService.enregistrerPointsQuestionnaire(id, parsePoints(params));
      recrutementService.calculerTop5(id);
      ra.addFlashAttribute("success", "Notes enregistrées et Top 5 calculé.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/offre/" + id + "/recrutement";
  }

  @PostMapping("/offre/{id}/entretien")
  public String entretien(@PathVariable Integer id, @RequestParam String nom,
      HttpSession session, RedirectAttributes ra) {
    authService.currentUser(session).orElseThrow();
    try {
      recrutementService.ajouterEntretien(id, nom);
      ra.addFlashAttribute("success", "Grille d'entretien ajoutée.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/offre/" + id + "/recrutement";
  }

  @PostMapping("/offre/{id}/notes-entretien")
  public String notesEntretien(@PathVariable Integer id, @RequestParam Integer entretienId,
      @RequestParam Map<String, String> params, HttpSession session, RedirectAttributes ra) {
    authService.currentUser(session).orElseThrow();
    try {
      recrutementService.enregistrerNotesEntretien(id, entretienId, parsePoints(params));
      recrutementService.calculerClassementFinal(id);
      ra.addFlashAttribute("success", "Notes d'entretien enregistrées et classement calculé.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/offre/" + id + "/recrutement";
  }

  private Map<Integer, BigDecimal> parsePoints(Map<String, String> params) {
    Map<Integer, BigDecimal> points = new HashMap<>();
    params.forEach((key, value) -> {
      if (key.startsWith("points_") && value != null && !value.isBlank()) {
        try {
          points.put(Integer.valueOf(key.substring(7)), new BigDecimal(value));
        } catch (NumberFormatException ignored) {
          throw new IllegalArgumentException("Une note est invalide.");
        }
      }
    });
    return points;
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
