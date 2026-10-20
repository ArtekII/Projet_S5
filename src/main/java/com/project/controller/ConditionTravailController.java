package com.project.controller;

import com.project.entity.ConditionTravail;
import com.project.entity.Poste;
import com.project.entity.Role;
import com.project.service.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConditionTravailController {
    private final ConditionTravailService service;
    private final AuthService auth;

    public ConditionTravailController(ConditionTravailService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping("/rh/conditions")
    public String rh(Model model) {
        model.addAttribute("postesConditions", service.postes());
        model.addAttribute("dossiers", service.dossiers(null));
        return "rh/conditions";
    }

    @GetMapping("/candidat/conditions")
    public String candidat(HttpSession session, Model model) {
        model.addAttribute("dossiers", service.dossiers(auth.currentUser(session).orElseThrow().getId()));
        model.addAttribute("reponsesConditions", service.reponsesConditions());
        return "candidat/conditions";
    }

    // Apercu visuel uniquement : aucune candidature ni condition n'est enregistree.
    @GetMapping("/candidat/conditions/apercu")
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
        model.addAttribute("reponsesConditions", service.reponsesConditions());
        model.addAttribute("dossiers", List.of(Map.of(
                "id", 0, "intitule", "Design UI/UX", "actuelle", condition,
                "initiale", condition, "historique", List.of())));
        return "candidat/conditions";
    }

    // Seuls les champs negociables peuvent etre lies depuis un formulaire.
    @InitBinder("condition")
    public void champs(WebDataBinder binder) {
        binder.setAllowedFields("salaire", "heuresParSemaine", "horaires", "lieuTravail", "remarque");
    }

    @PostMapping("/rh/conditions/poste/{id}")
    public String definir(@PathVariable Integer id, @Valid @ModelAttribute("condition") ConditionTravail condition,
            BindingResult erreurs, RedirectAttributes flash, Model model) {
        if (erreurs.hasErrors()) {
            model.addAttribute("error", "Verifiez les valeurs du formulaire.");
            model.addAttribute("posteErreur", id);
            return rh(model);
        }
        try {
            Poste poste = new Poste();
            poste.setId(id);
            service.definirConditionsTravail(poste, condition);
            flash.addFlashAttribute("success", "Conditions du poste enregistrees. Les negociations deja ouvertes restent inchangees.");
            return "redirect:/rh/conditions";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("posteErreur", id);
            return rh(model);
        }
    }

    @PostMapping("/candidat/conditions/candidature/{id}/proposer")
    public String proposer(@PathVariable Integer id, @Valid @ModelAttribute("condition") ConditionTravail condition,
            BindingResult erreurs, HttpSession session, RedirectAttributes flash, Model model) {
        if (erreurs.hasErrors()) {
            model.addAttribute("error", "Verifiez les valeurs du formulaire.");
            model.addAttribute("candidatureErreur", id);
            return candidat(session, model);
        }
        try {
            service.proposer(id, auth.currentUser(session).orElseThrow().getId(), condition);
            flash.addFlashAttribute("success", "Votre proposition a ete envoyee au RH pour validation.");
            return "redirect:/candidat/conditions";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("candidatureErreur", id);
            return candidat(session, model);
        }
    }

    @PostMapping("/rh/conditions/{id}/decision")
    public String decider(@PathVariable Integer id, @RequestParam boolean valider, RedirectAttributes flash) {
        try {
            service.decider(id, valider);
            flash.addFlashAttribute("success", valider ? "Changements valides." : "Changements refuses.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rh/conditions";
    }
}
