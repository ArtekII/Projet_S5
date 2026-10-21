package com.project.controller;

import com.project.entity.ConditionTravail;
import com.project.entity.Poste;
import com.project.service.ConditionTravailService;
import com.project.service.ContratService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
@RequestMapping("/rh")
public class RhController {
  private final OffreService offreService;
  private final ConditionTravailService conditionService;
  private final ContratService contratService;
  private final AuthService authService;

  public RhController(OffreService o, AuthService a, ConditionTravailService conditionService, ContratService contratService) {
    this.contratService = contratService;
    this.offreService = o;
    this.authService = a;
    this.conditionService = conditionService;
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("postes", offreService.postesActifs());
    model.addAttribute("offresAttente", offreService.offresEnAttenteRh());
    model.addAttribute("demandesAttente", offreService.demandesEnAttente());
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    model.addAttribute("toutesDemandes", offreService.toutesDemandes());
    return "rh/dashboard";
  }

  @PostMapping("/offre/{id}/valider")
  public String validerOffre(@PathVariable Integer id,
      @RequestParam(required = false) Integer posteId,
      @RequestParam(required = false) Integer anneesExperience,
      @RequestParam(required = false) Integer nbPlaces,
      @RequestParam BigDecimal salaire,
      @RequestParam(required = false) String commentaire,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.rhValiderOffre(id, posteId, anneesExperience, nbPlaces, salaire, u, true, commentaire);
      ra.addFlashAttribute("success", "Offre validee -> transmise a la Finance.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/offre/{id}/refuser")
  public String refuserOffre(@PathVariable Integer id, @RequestParam(required = false) String commentaire,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      offreService.rhValiderOffre(id, null, null, null, null, u, false, commentaire);
      ra.addFlashAttribute("success", "Offre refusee par RH.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/demande/{id}/valider")
  public String validerDemande(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    offreService.traiterDemandePoste(id, true, null, u);
    ra.addFlashAttribute("success", "Poste ajoute au catalogue (dropdown manager).");
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/demande/{id}/refuser")
  public String refuserDemande(@PathVariable Integer id, @RequestParam(required = false) String motif,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    offreService.traiterDemandePoste(id, false, motif, u);
    ra.addFlashAttribute("success", "Demande de poste refusee.");
    return "redirect:/rh/dashboard";
  }

    // Conditions de travail.
    @InitBinder("condition")
    public void champs(WebDataBinder binder) {
        binder.setAllowedFields("salaire", "heuresParSemaine", "horaires", "lieuTravail", "remarque");
    }

    @GetMapping("/conditions")
    public String conditions(Model model) {
        model.addAttribute("postesConditions", conditionService.postes());
        model.addAttribute("dossiers", conditionService.dossiers(null));
        return "rh/conditions";
    }

    @PostMapping("/conditions/poste/{id}")
    public String definir(@PathVariable Integer id, @Valid @ModelAttribute("condition") ConditionTravail condition,
            BindingResult erreurs, RedirectAttributes flash, Model model) {
        if (erreurs.hasErrors()) {
            model.addAttribute("error", "Verifiez les valeurs du formulaire.");
            model.addAttribute("posteErreur", id);
            return conditions(model);
        }
        try {
            Poste poste = new Poste();
            poste.setId(id);
            conditionService.definirConditionsTravail(poste, condition);
            flash.addFlashAttribute("success", "Conditions du poste enregistrees. Les negociations deja ouvertes restent inchangees.");
            return "redirect:/rh/conditions";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("posteErreur", id);
            return conditions(model);
        }
    }

    @PostMapping("/conditions/{id}/decision")
    public String decider(@PathVariable Integer id, @RequestParam boolean valider, RedirectAttributes flash) {
        try {
            conditionService.decider(id, valider);
            flash.addFlashAttribute("success", valider ? "Changements valides." : "Changements refuses.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rh/conditions";
    }
    @PostMapping("/candidatures/{id}/autoriser-cdi")
    public String autoriserCdi(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        try {
            contratService.autoriserCdi(id, authService.currentUser(session).orElseThrow().getId());
            flash.addFlashAttribute("success", "CDI autorise pour cette candidature.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rh/conditions";
    }

    @PostMapping("/conditions/candidature/{id}/prolonger")
    public String prolonger(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            conditionService.prolonger(id);
            flash.addFlashAttribute("success", "Delai de reponse prolonge.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rh/conditions";
    }

    @GetMapping("/contrats")
    public String contrats(Model model) {
        model.addAttribute("contrats", contratService.tousLesContrats());
        return "rh/contrats";
    }

    @PostMapping("/contrats/{id}/preparer")
    public String preparerContrat(@PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            HttpSession session, RedirectAttributes flash) {
        try {
            contratService.preparer(id, dateDebut, dateFin, authService.currentUser(session).orElseThrow().getId());
            flash.addFlashAttribute("success", "Contrat prepare et disponible pour signature.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rh/contrats";
    }

}
