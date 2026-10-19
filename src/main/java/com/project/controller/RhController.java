package com.project.controller;

import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
@RequestMapping("/rh")
public class RhController {
  private final OffreService offreService;
  private final AuthService authService;

  public RhController(OffreService o, AuthService a) {
    this.offreService = o;
    this.authService = a;
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
    return "rh/dashboard";
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
