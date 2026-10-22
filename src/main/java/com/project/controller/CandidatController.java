package com.project.controller;

import com.project.entity.ConditionTravail;
import com.project.entity.Role;
import com.project.entity.Contrat;
import com.project.service.ConditionTravailService;
import com.project.service.ContratService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/candidat")
public class CandidatController {
  private final OffreService offreService;
  private final ConditionTravailService conditionService;
  private final ContratService contratService;
  private final AuthService authService;

  public CandidatController(OffreService o, AuthService a, ConditionTravailService conditionService, ContratService contratService) {
    this.offreService = o;
    this.authService = a;
    this.conditionService = conditionService;
    this.contratService = contratService;
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

    // Conditions de travail et contrats.
    @InitBinder("condition")
    public void champs(WebDataBinder binder) {
        binder.setAllowedFields("salaire", "heuresParSemaine", "horaires", "lieuTravail", "remarque");
    }

    @GetMapping("/conditions")
    public String conditions(HttpSession session, Model model) {
        Integer candidatId = authService.currentUser(session).orElseThrow().getId();
        model.addAttribute("dossiers", conditionService.dossiers(candidatId));
        model.addAttribute("contrats", contratService.contratsDuCandidat(candidatId));
        model.addAttribute("reponsesConditions", conditionService.reponsesConditions());
        return "candidat/conditions";
    }

    @GetMapping("/conditions/apercu")
    public String apercu(Model model) {
        ConditionTravail condition = new ConditionTravail();
        condition.setSalaire(new BigDecimal("1500000.00"));
        condition.setHeuresParSemaine(new BigDecimal("40.0"));
        condition.setHoraires("Lundi au vendredi, 8 h a 17 h");
        condition.setLieuTravail("Antananarivo");
        Role rh = new Role();
        rh.setCode("RH");
        condition.setRoleProposant(rh);
        model.addAttribute("apercu", true);
        model.addAttribute("reponsesConditions", conditionService.reponsesConditions());
        model.addAttribute("dossiers", List.of(Map.of(
                "id", 0, "intitule", "Design UI/UX", "actuelle", condition,
                "initiale", condition, "historique", List.of())));
        return "candidat/conditions";
    }

    @PostMapping("/conditions/candidature/{id}/proposer")
    public String proposer(@PathVariable Integer id, @Valid @ModelAttribute("condition") ConditionTravail condition,
            BindingResult erreurs, HttpSession session, RedirectAttributes flash, Model model) {
        if (erreurs.hasErrors()) {
            model.addAttribute("error", "Verifiez les valeurs du formulaire.");
            model.addAttribute("candidatureErreur", id);
            return conditions(session, model);
        }
        try {
            conditionService.proposer(id, authService.currentUser(session).orElseThrow().getId(), condition);
            flash.addFlashAttribute("success", "Votre proposition a ete envoyee au RH pour validation.");
            return "redirect:/candidat/conditions";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("candidatureErreur", id);
            return conditions(session, model);
        }
    }

    @PostMapping("/conditions/candidature/{id}/refuser")
    public String refuser(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        try {
            conditionService.refuser(id, authService.currentUser(session).orElseThrow().getId());
            flash.addFlashAttribute("success", "Vous avez refuse les conditions. Votre candidature est cloturee.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/candidat/conditions";
    }

    @GetMapping("/contrats")
    public String contrats(HttpSession session, Model model) {
        return "redirect:/candidat/conditions";
    }

    @PostMapping("/conditions/candidature/{id}/accepter")
    public String accepter(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        try {
            Contrat contrat = contratService.accepterConditions(id, authService.currentUser(session).orElseThrow().getId());
            flash.addFlashAttribute("success", "Conditions acceptees. Votre contrat " + contrat.getTypeContrat().getCode() + " est cree.");
            return "redirect:/candidat/conditions";
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/candidat/conditions";
        }
    }
    @PostMapping("/conditions/candidature/{id}/accepter-initiales")
    public String accepterInitiales(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        try {
            Contrat contrat = contratService.accepterConditionsInitiales(id, authService.currentUser(session).orElseThrow().getId());
            flash.addFlashAttribute("success", "Conditions RH initiales acceptees. Votre contrat " + contrat.getTypeContrat().getCode() + " est cree.");
            return "redirect:/candidat/conditions";
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/candidat/conditions";
        }
    }

}
