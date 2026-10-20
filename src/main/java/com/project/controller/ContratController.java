package com.project.controller;

import com.project.entity.Contrat;
import com.project.service.AuthService;
import com.project.service.ContratService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/candidat")
public class ContratController {
    private final ContratService contrats;
    private final AuthService auth;

    public ContratController(ContratService contrats, AuthService auth) {
        this.contrats = contrats;
        this.auth = auth;
    }

    @GetMapping("/contrats")
    public String contrats(HttpSession session, Model model) {
        model.addAttribute("contrats", contrats.contratsDuCandidat(auth.currentUser(session).orElseThrow().getId()));
        return "candidat/contrats";
    }

    @PostMapping("/conditions/candidature/{id}/accepter")
    public String accepter(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        try {
            Contrat contrat = contrats.accepterConditions(id, auth.currentUser(session).orElseThrow().getId());
            flash.addFlashAttribute("success", "Conditions acceptees. Votre contrat " + contrat.getTypeContrat().getCode() + " est cree.");
            return "redirect:/candidat/contrats";
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/candidat/conditions";
        }
    }
}
