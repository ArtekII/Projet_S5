package com.project.controller;

import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager")
public class ManagerController {
  private final OffreService offreService;
  private final AuthService authService;

  public ManagerController(OffreService o, AuthService a) {
    this.offreService = o;
    this.authService = a;
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("postes", offreService.postesActifs());
    model.addAttribute("mesOffres", offreService.offresDuManager(u.getId()));
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    return "manager/dashboard";
  }

  @PostMapping("/offre")
  public String creerOffre(@RequestParam Integer posteId, @RequestParam int anneesExperience,
      @RequestParam int nbPlaces, HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    if (nbPlaces <= 0 || anneesExperience < 0) {
      ra.addFlashAttribute("error", "Valeurs invalides.");
      return "redirect:/manager/dashboard";
    }
    offreService.creerOffre(posteId, anneesExperience, nbPlaces, u);
    ra.addFlashAttribute("success", "Demande d\u0027offre envoy\u00e9e au RH (en attente RH).");
    return "redirect:/manager/dashboard";
  }

  @PostMapping("/demande-poste")
  public String demanderPoste(@RequestParam String intitule, @RequestParam(required = false) String justification,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    if (intitule == null || intitule.isBlank()) {
      ra.addFlashAttribute("error", "Intitule requis.");
      return "redirect:/manager/dashboard";
    }
    offreService.demanderNouveauPoste(intitule.trim(), justification, u);
    ra.addFlashAttribute("success", "Demande de nouveau poste envoy\u00e9e au RH.");
    return "redirect:/manager/dashboard";
  }

  @GetMapping("/demandes")
  public String mesDemandes(HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("demandes",
        offreService.toutesDemandes().stream().filter(d -> d.getManager().getId().equals(u.getId())).toList());
    return "manager/demandes";
  }
}
