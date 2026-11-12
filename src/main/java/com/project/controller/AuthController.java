package com.project.controller;

import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService a) {
    this.authService = a;
  }

  @GetMapping({ "/", "/login" })
  public String loginPage(@RequestParam(value = "error", required = false) String error, Model model,
      @RequestParam(required = false) Integer offre,
      HttpSession session) {
    if (session != null && session.getAttribute("userId") != null) {
      String role = (String) session.getAttribute("userRole");
      return offre != null && "CANDIDAT".equals(role) ? "redirect:/espaceCandidat?offre=" + offre
          : authService.redirectAfterLogin((Integer) session.getAttribute("userId"), role);
    }
    if (error != null)
      model.addAttribute("error",
          error.equals("role") ? "Accès non autorisé pour votre rôle." : "Email ou mot de passe incorrect.");
    model.addAttribute("offreSouhaitee", offre);
    return "login";
  }

  @PostMapping("/login")
  public String doLogin(@RequestParam String email, @RequestParam String password,
      @RequestParam(required = false) Integer offre, HttpSession session, Model model) {
    Optional<Utilisateur> opt = authService.authenticate(email, password);
    if (opt.isEmpty()) {
      model.addAttribute("error", "Email ou mot de passe incorrect.");
      model.addAttribute("email", email);
      model.addAttribute("offreSouhaitee", offre);
      return "login";
    }
    authService.login(session, opt.get());
    if (offre != null && "CANDIDAT".equals(opt.get().getRole().getCode())) return "redirect:/espaceCandidat?offre=" + offre;
    return authService.redirectAfterLogin(opt.get().getId(), opt.get().getRole().getCode());
  }

  @GetMapping("/logout")
  public String logout(HttpSession session) {
    if (session != null)
      authService.logout(session);
    return "redirect:/login";
  }
}
