package com.project.service;

import com.project.entity.Utilisateur;
import com.project.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {
  private final UtilisateurRepository utilisateurRepository;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(UtilisateurRepository ur) {
    this.utilisateurRepository = ur;
  }

  public Optional<Utilisateur> authenticate(String email, String rawPassword) {
    Optional<Utilisateur> opt = utilisateurRepository.findByEmail(email);
    if (opt.isEmpty())
      return Optional.empty();
    Utilisateur u = opt.get();
    if (!encoder.matches(rawPassword, u.getMotDePasse()))
      return Optional.empty();
    if (!Boolean.TRUE.equals(u.getActif()))
      return Optional.empty();
    return Optional.of(u);
  }

  public void login(HttpSession session, Utilisateur u) {
    session.setAttribute("userId", u.getId());
    session.setAttribute("userEmail", u.getEmail());
    session.setAttribute("userRole", u.getRole().getCode());
    session.setAttribute("userRoleLibelle", u.getRole().getLibelle());
  }

  public void logout(HttpSession session) {
    session.invalidate();
  }

  public Optional<Utilisateur> currentUser(HttpSession session) {
    Object id = session.getAttribute("userId");
    if (id == null)
      return Optional.empty();
    return utilisateurRepository.findById((Integer) id);
  }

  public String redirectByRole(String roleCode) {
    return switch (roleCode) {
      case "MANAGER" -> "redirect:/manager/dashboard";
      case "RH" -> "redirect:/rh/dashboard";
      case "FINANCE" -> "redirect:/finance/dashboard";
      case "DIRECTION" -> "redirect:/direction/dashboard";
      case "CANDIDAT" -> "redirect:/candidat/offres";
      default -> "redirect:/login";
    };
  }
}
