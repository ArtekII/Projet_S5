package com.project.controller;

import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import com.project.service.CandidatureService;
import com.project.service.RecrutementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Controller
@RequestMapping("/candidat")
public class CandidatController {
  private final OffreService offreService;
  private final AuthService authService;
  private final CandidatureService candidatureService;
  private final RecrutementService recrutementService;

  public CandidatController(OffreService o, AuthService a, CandidatureService c, RecrutementService r) {
    this.offreService = o;
    this.authService = a;
    this.candidatureService = c;
    this.recrutementService = r;
  }

  @GetMapping("/offres")
  public String offres(HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offres", offreService.offresPubliees());
    return "candidat/offres";
  }

  @GetMapping("/offre/{id}")
  public String detail(@PathVariable Integer id, HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offre", offreService.findOffre(id).orElseThrow());
    model.addAttribute("validations", offreService.validations(id));
    model.addAttribute("aDejaCandidate", candidatureService.aDejaCandidate(u.getId(), id));
    model.addAttribute("selectionnePourQuestionnaire", recrutementService.candidatSelectionne(u.getId(), id));
    return "candidat/detail";
  }

  @GetMapping("/offre/{id}/questionnaire")
  public String questionnaire(@PathVariable Integer id, HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    if (!recrutementService.candidatSelectionne(u.getId(), id))
      throw new IllegalStateException("Vous n'êtes pas sélectionné pour cette étape.");
    model.addAttribute("offre", offreService.findOffre(id).orElseThrow());
    model.addAttribute("questions", recrutementService.questions(id));
    return "candidat/questionnaire";
  }

  @PostMapping("/offre/{id}/candidature")
  public String candidater(@PathVariable Integer id,
      @RequestParam String nom,
      @RequestParam String prenom,
      @RequestParam LocalDate dateNaissance,
      @RequestParam Integer anneeExperience,
      HttpSession session,
      RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    if (nom.isBlank() || prenom.isBlank() || anneeExperience < 0) {
      ra.addFlashAttribute("error", "Veuillez remplir correctement tous les champs.");
      return "redirect:/candidat/offre/" + id;
    }
    try {
      candidatureService.enregistrer(id, u, nom, prenom, dateNaissance, anneeExperience);
      ra.addFlashAttribute("success", "Votre candidature a été enregistrée.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/candidat/offre/" + id;
  }
}
