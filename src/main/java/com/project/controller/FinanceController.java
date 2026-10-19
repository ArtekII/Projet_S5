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
@RequestMapping("/finance")
public class FinanceController {
  private final OffreService offreService;
  private final AuthService authService;

  public FinanceController(OffreService o, AuthService a) {
    this.offreService = o;
    this.authService = a;
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offresAttente", offreService.offresEnAttenteFinance());
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    return "finance/dashboard";
  }

  @PostMapping("/offre/{id}/valider")
  public String valider(@PathVariable Integer id, @RequestParam(required = false) BigDecimal salaire,
      @RequestParam(required = false) String commentaire, HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.financeValiderOffre(id, salaire, u, true, commentaire);
      ra.addFlashAttribute("success", "Offre validee -> transmise a la Direction.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/finance/dashboard";
  }

  @PostMapping("/offre/{id}/refuser")
  public String refuser(@PathVariable Integer id, @RequestParam(required = false) String commentaire,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.financeValiderOffre(id, null, u, false, commentaire);
      ra.addFlashAttribute("success", "Offre refusee par Finance.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/finance/dashboard";
  }
}
