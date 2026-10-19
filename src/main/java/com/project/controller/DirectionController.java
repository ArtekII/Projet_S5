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
@RequestMapping("/direction")
public class DirectionController {
  private final OffreService offreService;
  private final AuthService authService;

  public DirectionController(OffreService o, AuthService a) {
    this.offreService = o;
    this.authService = a;
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offresAttente", offreService.offresEnAttenteDirection());
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    model.addAttribute("offresPubliees", offreService.offresPubliees());
    return "direction/dashboard";
  }

  @PostMapping("/offre/{id}/valider")
  public String valider(@PathVariable Integer id, @RequestParam(required = false) String commentaire,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.directionValiderOffre(id, u, true, commentaire);
      ra.addFlashAttribute("success", "Offre PUBLIEe ! Visible par les candidats.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/direction/dashboard";
  }

  @PostMapping("/offre/{id}/refuser")
  public String refuser(@PathVariable Integer id, @RequestParam(required = false) String commentaire,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.directionValiderOffre(id, u, false, commentaire);
      ra.addFlashAttribute("success", "Offre refusee par Direction.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/direction/dashboard";
  }
}
