package com.project.controller;

import com.project.entity.DossierAdministratif;
import com.project.entity.DocumentAdministratif;
import com.project.entity.ParcoursProfessionnel;
import com.project.service.CongeService;
import com.project.service.DossierAdministratifService;
import com.project.service.PointageService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DossierAdministratifController {
    private final DossierAdministratifService service;
    private final PointageService pointages;
    private final CongeService conges;

    public DossierAdministratifController(DossierAdministratifService service, PointageService pointages,
            CongeService conges) {
        this.service = service; this.pointages = pointages; this.conges = conges;
    }

    @InitBinder("fiche")
    void ficheBinder(WebDataBinder binder) {
        binder.setAllowedFields("revision", "nom", "prenom", "dateNaissance", "lieuNaissance", "nationalite", "numeroIdentite",
                "adresse", "telephone", "emailPersonnel", "situationMatrimoniale", "nombreEnfants", "contactUrgence", "telephoneUrgence");
    }

    @InitBinder("nouveauParcours")
    void parcoursBinder(WebDataBinder binder) {
        binder.setAllowedFields("type", "libelle", "dateDebut", "dateFin", "montant", "description");
    }

    @ModelAttribute("dossierCsrf")
    String jeton(HttpSession session, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String token = (String) session.getAttribute("dossierCsrf");
        if (token == null) {
            token = UUID.randomUUID().toString(); session.setAttribute("dossierCsrf", token);
        }
        return token;
    }

    private Integer acteur(HttpSession session) {
        if (!(session.getAttribute("userId") instanceof Integer id)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return id;
    }

    private Integer cible(Integer utilisateurId, HttpSession session) {
        return utilisateurId == null ? acteur(session) : utilisateurId;
    }

    private String chemin(Integer utilisateurId) {
        return utilisateurId == null ? "/espaceEmploye" : "/rh/dossiers/" + utilisateurId;
    }

    private void verifierJeton(String token, HttpSession session) {
        String attendu = (String) session.getAttribute("dossierCsrf");
        if (token == null || attendu == null || !MessageDigest.isEqual(attendu.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Formulaire expiré. Rechargez la page.");
        }
    }

    private String afficher(Integer utilisateurId, HttpSession session, Model model) {
        Map<String, Object> vue = service.consulter(cible(utilisateurId, session), acteur(session));
        vue.forEach((cle, valeur) -> { if (!model.containsAttribute(cle)) model.addAttribute(cle, valeur); });
        boolean personnel = utilisateurId == null;
        model.addAttribute("espaceEmploye", personnel);
        model.addAttribute("gestionRh", !personnel && Boolean.TRUE.equals(vue.get("gestionRh")));
        if (!model.containsAttribute("ongletEmploye")) model.addAttribute("ongletEmploye", "renseignements");
        if (personnel && "pointages".equals(model.getAttribute("ongletEmploye"))) {
            model.addAllAttributes(pointages.espaceUtilisateur(acteur(session)));
        }
        if (personnel && "conges".equals(model.getAttribute("ongletEmploye"))) {
            model.addAllAttributes(conges.espaceEmploye(acteur(session)));
        }
        model.addAttribute("baseDossier", chemin(utilisateurId));
        model.addAttribute("typesDocument", DossierAdministratifService.TYPES_DOCUMENT);
        if (!model.containsAttribute("nouveauParcours")) model.addAttribute("nouveauParcours", new ParcoursProfessionnel());
        return personnel ? "employe/espaceEmploye" : "dossier/detail";
    }

    @GetMapping("/rh/dossiers")
    String liste(HttpSession session, Model model) {
        model.addAttribute("dossiers", service.lister(acteur(session)));
        return "dossier/liste";
    }

    @GetMapping({"/espaceEmploye", "/dossier", "/rh/dossiers/{utilisateurId}"})
    String detail(@PathVariable(required = false) Integer utilisateurId,
            @RequestParam(defaultValue = "renseignements") String onglet, HttpSession session, Model model) {
        model.addAttribute("ongletEmploye", Set.of("renseignements", "documents", "cv", "contrats", "parcours", "integration", "pointages", "conges").contains(onglet)
                ? onglet : "renseignements");
        return afficher(utilisateurId, session, model);
    }

    @PostMapping({"/espaceEmploye/fiche", "/dossier/fiche", "/rh/dossiers/{utilisateurId}/fiche"})
    String fiche(@PathVariable(required = false) Integer utilisateurId, HttpSession session,
            @RequestParam(required = false) String dossierCsrf,
            @Valid @ModelAttribute("fiche") DossierAdministratif fiche, BindingResult erreurs,
            Model model, RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        if (erreurs.hasErrors()) return afficher(utilisateurId, session, model);
        try {
            service.enregistrerFiche(cible(utilisateurId, session), acteur(session), fiche);
            flash.addFlashAttribute("success", "Fiche de renseignements enregistrée.");
            return "redirect:" + chemin(utilisateurId);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return afficher(utilisateurId, session, model);
        }
    }

    @PostMapping({"/espaceEmploye/documents", "/dossier/documents", "/rh/dossiers/{utilisateurId}/documents"})
    String deposer(@PathVariable(required = false) Integer utilisateurId, HttpSession session,
            @RequestParam(required = false) String dossierCsrf, @RequestParam String type,
            @RequestParam(required = false) MultipartFile fichier,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDelivrance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateExpiration,
            RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        try {
            service.deposer(cible(utilisateurId, session), acteur(session), type, fichier, dateDelivrance, dateExpiration);
            flash.addFlashAttribute("success", "Document déposé. Les dépôts précédents sont conservés.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (IOException ex) {
            flash.addFlashAttribute("error", "Lecture du fichier impossible. Réessayez le dépôt.");
        }
        return "redirect:" + chemin(utilisateurId) + (utilisateurId == null ? "?onglet=documents" : "");
    }

    @GetMapping({"/espaceEmploye/documents/{documentId}", "/dossier/documents/{documentId}", "/rh/dossiers/{utilisateurId}/documents/{documentId}"})
    ResponseEntity<byte[]> telecharger(@PathVariable(required = false) Integer utilisateurId,
            @PathVariable Integer documentId, HttpSession session) {
        DocumentAdministratif document = service.telecharger(cible(utilisateurId, session), acteur(session), documentId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.getNomFichier(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore()).body(document.getContenu());
    }

    @PostMapping("/rh/dossiers/{utilisateurId}/documents/{documentId}/validite")
    String validite(@PathVariable Integer utilisateurId, @PathVariable Integer documentId, HttpSession session,
            @RequestParam(required = false) String dossierCsrf,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDelivrance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateExpiration,
            RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        try {
            service.modifierValidite(utilisateurId, acteur(session), documentId, dateDelivrance, dateExpiration);
            flash.addFlashAttribute("success", "Validité du document mise à jour.");
        } catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:" + chemin(utilisateurId);
    }

    @PostMapping("/rh/dossiers/{utilisateurId}/parcours")
    String parcours(@PathVariable Integer utilisateurId, HttpSession session,
            @RequestParam(required = false) String dossierCsrf,
            @Valid @ModelAttribute("nouveauParcours") ParcoursProfessionnel parcours, BindingResult erreurs,
            Model model, RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        if (erreurs.hasErrors()) return afficher(utilisateurId, session, model);
        try {
            service.ajouterParcours(utilisateurId, acteur(session), parcours);
            flash.addFlashAttribute("success", "Événement professionnel ajouté à l'historique.");
            return "redirect:" + chemin(utilisateurId);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return afficher(utilisateurId, session, model);
        }
    }

    @PostMapping("/rh/dossiers/{utilisateurId}/integration")
    String integration(@PathVariable Integer utilisateurId, HttpSession session,
            @RequestParam(required = false) String dossierCsrf, @RequestParam(required = false) Long revision,
            @RequestParam(defaultValue = "") Set<String> etapes, RedirectAttributes flash) {
        verifierJeton(dossierCsrf, session);
        try {
            service.enregistrerIntegration(utilisateurId, acteur(session), revision, etapes);
            flash.addFlashAttribute("success", "Checklist d'intégration enregistrée.");
        } catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:" + chemin(utilisateurId);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<String> fichierTropVolumineux() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body("Le fichier dépasse la limite de 10 Mo. Revenez au dossier pour choisir un fichier plus petit.");
    }
}
