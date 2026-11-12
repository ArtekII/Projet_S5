package com.project.controller;

import com.project.dto.PlanningForm;
import com.project.entity.VariablePaiePointage;
import com.project.service.PointageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PointageController {
    private final PointageService service;
    public PointageController(PointageService service) { this.service = service; }

    @InitBinder("planningForm")
    void champs(WebDataBinder binder) {
        binder.setAllowedFields("posteId", "jourSemaine", "dateEffet", "jourTravaille", "heureDebut", "heureFin", "pauseDebut", "pauseFin");
    }

    @ModelAttribute("pointageCsrf")
    String jeton(HttpSession session, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (session.getAttribute("pointageCsrf") == null) session.setAttribute("pointageCsrf", UUID.randomUUID().toString());
        return (String) session.getAttribute("pointageCsrf");
    }

    private Integer acteur(HttpSession session) {
        if (!(session.getAttribute("userId") instanceof Integer id)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return id;
    }

    private void verifierJeton(String jeton, HttpSession session) {
        String attendu = (String) session.getAttribute("pointageCsrf");
        if (jeton == null || attendu == null || !MessageDigest.isEqual(jeton.getBytes(StandardCharsets.UTF_8), attendu.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rechargez le formulaire avant de réessayer.");
        }
    }

    @GetMapping("/rh/plannings")
    String plannings(HttpSession session, Model model) {
        model.addAttribute("postes", service.postesRh(acteur(session)));
        model.addAttribute("plannings", service.historiquePlannings(acteur(session)));
        if (!model.containsAttribute("planningForm")) {
            PlanningForm formulaire = new PlanningForm(); formulaire.setDateEffet(service.aujourdhui());
            model.addAttribute("planningForm", formulaire);
        }
        return "pointage/plannings";
    }

    @PostMapping("/rh/plannings")
    String definir(@RequestParam(required = false) String pointageCsrf, HttpSession session,
            @Valid @ModelAttribute("planningForm") PlanningForm planning, BindingResult erreurs, Model model, RedirectAttributes flash) {
        verifierJeton(pointageCsrf, session);
        if (erreurs.hasErrors()) return plannings(session, model);
        try {
            service.definirPlanning(acteur(session), planning);
            flash.addFlashAttribute("success", "Nouvelle version du planning enregistrée.");
            return "redirect:/rh/plannings";
        } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); return plannings(session, model); }
    }

    @GetMapping("/rh/pointages")
    String rapport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au, HttpSession session, Model model) {
        LocalDate debut = du == null ? service.aujourdhui() : du;
        LocalDate fin = au == null ? debut : au;
        model.addAttribute("du", debut); model.addAttribute("au", fin);
        try { model.addAttribute("bilans", service.rapportRh(acteur(session), debut, fin)); }
        catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        return "pointage/rh";
    }

    @PostMapping("/rh/pointages/{id}/corriger")
    String corriger(@PathVariable Long id, @RequestParam(required = false) String pointageCsrf,
            @RequestParam Long revision, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entree,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sortie,
            @RequestParam String motif, HttpSession session, RedirectAttributes flash) {
        verifierJeton(pointageCsrf, session);
        try { service.corriger(acteur(session), id, revision, entree, sortie, motif); flash.addFlashAttribute("success", "Pointage régularisé ; les horaires d'origine restent conservés."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/rh/pointages?du=" + entree.toLocalDate() + "&au=" + entree.toLocalDate();
    }

    @PostMapping("/rh/pointages/transmettre")
    String transmettre(@RequestParam(required = false) String pointageCsrf, @RequestParam Integer employeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, HttpSession session, RedirectAttributes flash) {
        verifierJeton(pointageCsrf, session);
        try { service.transmettre(acteur(session), employeId, date); flash.addFlashAttribute("success", "Variables validées et transmises à Finance."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/rh/pointages?du=" + date + "&au=" + date;
    }

    @GetMapping("/finance/pointages")
    String finance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au, HttpSession session, Model model) {
        LocalDate debut = du == null ? service.aujourdhui().withDayOfMonth(1) : du;
        LocalDate fin = au == null ? debut.withDayOfMonth(debut.lengthOfMonth()) : au;
        model.addAttribute("du", debut); model.addAttribute("au", fin);
        try { model.addAttribute("variables", service.variablesFinance(acteur(session), debut, fin)); }
        catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        return "pointage/finance";
    }

    @GetMapping("/finance/pointages.csv")
    ResponseEntity<byte[]> exporter(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au, HttpSession session) {
        StringBuilder csv = new StringBuilder("\uFEFFemploye;date;prevu_secondes;travail_secondes;retard_secondes;heures_sup_secondes;absence_secondes;valide_par;transmis_le\r\n");
        try {
            for (VariablePaiePointage v : service.variablesFinance(acteur(session), du, au)) {
                csv.append(v.getEmploye().getId()).append(';').append(v.getDate()).append(';').append(v.getPrevuSecondes()).append(';')
                        .append(v.getTravailSecondes()).append(';').append(v.getRetardSecondes()).append(';')
                        .append(v.getSupplementairesSecondes()).append(';').append(v.getAbsenceSecondes()).append(';')
                        .append(v.getRh().getId()).append(';').append(v.getTransmisLe()).append("\r\n");
            }
        } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=variables-pointage.csv")
                .cacheControl(CacheControl.noStore()).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
