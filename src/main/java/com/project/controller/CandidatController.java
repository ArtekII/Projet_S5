package com.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/candidat")
public class CandidatController {
  private final OffreService offreService;
  private final AuthService authService;

  public CandidatController(OffreService o, AuthService a) {
    this.offreService = o;
    this.authService = a;
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
    return "candidat/detail";
  }
}
