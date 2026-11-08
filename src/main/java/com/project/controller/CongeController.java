package com.project.controller;

import com.project.service.CongeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CongeController {
    private final CongeService service;

    public CongeController(CongeService service) {
        this.service = service;
    }

    @ModelAttribute("dossierCsrf")
    String jeton(HttpSession session, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String token = (String) session.getAttribute("dossierCsrf");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("dossierCsrf", token);
        }
        return token;
    }

    private Integer acteur(HttpSession session) {
        if (!(session.getAttribute("userId") instanceof Integer id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }

    private void verifierJeton(String token, HttpSession session) {
        String attendu = (String) session.getAttribute("dossierCsrf");
        if (token == null || attendu == null
                || !MessageDigest.isEqual(attendu.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Formulaire expiré. Rechargez la page.");
        }
    }

    @PostMapping("/espaceEmploye/conges")
    String demander(@RequestParam(required = false) Integer typeCongeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) String commentaire,
            @RequestParam(required = false) String dossierCsrf,
            HttpSession session, RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        try {
            if (typeCongeId == null) {
                throw new IllegalArgumentException("Choisissez un type de congé.");
            }
            service.demander(acteur(session), typeCongeId, dateDebut, dateFin, commentaire);
            flash.addFlashAttribute("success", "Votre demande de congé a été envoyée au RH.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/espaceEmploye?onglet=conges";
    }

    @PostMapping("/espaceEmploye/conges/{id}/annuler")
    String annuler(@PathVariable Integer id, @RequestParam(required = false) String dossierCsrf,
            HttpSession session, RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        try {
            service.annuler(acteur(session), id);
            flash.addFlashAttribute("success", "La demande de congé a été annulée.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/espaceEmploye?onglet=conges";
    }

    @GetMapping("/rh/conges")
    String espaceRh(HttpSession session, Model model) {
        service.espaceRh(acteur(session)).forEach(model::addAttribute);
        return "rh/conges";
    }

    @PostMapping("/rh/conges/{id}/decision")
    String decider(@PathVariable Integer id, @RequestParam String action,
            @RequestParam(required = false) String commentaire,
            @RequestParam(required = false) String dossierCsrf,
            HttpSession session, RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        try {
            if ("VALIDER".equalsIgnoreCase(action)) {
                service.valider(acteur(session), id, commentaire);
                flash.addFlashAttribute("success", "Demande de congé validée.");
            } else if ("REFUSER".equalsIgnoreCase(action)) {
                service.refuser(acteur(session), id, commentaire);
                flash.addFlashAttribute("success", "Demande de congé refusée.");
            } else {
                throw new IllegalArgumentException("Action inconnue.");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/rh/conges";
    }
}
