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
      HttpSession session) {
    if (session != null && session.getAttribute("userId") != null) {
      String role = (String) session.getAttribute("userRole");
      return authService.redirectByRole(role);
    }
    if (error != null)
      model.addAttribute("error",
          error.equals("role") ? "Accès non autorisé pour votre rôle." : "Email ou mot de passe incorrect.");
    return "login";
  }

  @PostMapping("/login")
  public String doLogin(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
    Optional<Utilisateur> opt = authService.authenticate(email, password);
    if (opt.isEmpty()) {
      model.addAttribute("error", "Email ou mot de passe incorrect.");
      return "login";
    }
    authService.login(session, opt.get());
    return authService.redirectByRole(opt.get().getRole().getCode());
  }

  @GetMapping("/logout")
  public String logout(HttpSession session) {
    if (session != null)
      authService.logout(session);
    return "redirect:/login";
  }
}
