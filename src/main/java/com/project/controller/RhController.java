package com.project.controller;

import com.project.dto.CandidatureRhVue;
import com.project.dto.QuestionnaireVue;
import com.project.entity.Candidature;
import com.project.entity.Question;
import com.project.entity.Questionnaire;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.CandidatureService;
import com.project.service.OffreService;
import com.project.service.QuestionnaireService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rh")
public class RhController {
  private final OffreService offreService;
  private final AuthService authService;
  private final CandidatureService candidatureService;
  private final QuestionnaireService questionnaireService;

  public RhController(OffreService o, AuthService a, CandidatureService c,
      QuestionnaireService q) {
    this.offreService = o;
    this.authService = a;
    this.candidatureService = c;
    this.questionnaireService = q;
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
    model.addAttribute("candidaturesRh", buildCandidaturesRh());
    Map<Integer, Questionnaire> questionnairesParOffre = new HashMap<>();
    for (Questionnaire questionnaire : questionnaireService.getQuestionnaires()) {
      questionnairesParOffre.put(questionnaire.getOffre().getId(), questionnaire);
    }
    model.addAttribute("questionnairesParOffre", questionnairesParOffre);
    return "rh/dashboard";
  }

  @GetMapping("/offre/{id}/candidats")
  public String candidatsParOffre(@PathVariable Integer id, HttpSession session, Model model) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    Questionnaire questionnaire = questionnaireService.getQuestionnaire(id).orElse(null);
    List<Question> questions = questionnaire == null ? List.of()
        : questionnaireService.getQuestions(questionnaire.getId());
    List<CandidatureRhVue> candidatures = buildCandidatures(
        candidatureService.getCandidaturesByOffre(id), questionnaire, questions);
    model.addAttribute("user", u);
    model.addAttribute("offre", offreService.toutesOffres().stream()
        .filter(offre -> id.equals(offre.getId())).findFirst().orElseThrow());
    model.addAttribute("candidaturesRh", candidatures);
    model.addAttribute("questionnaire", questionnaire);
    model.addAttribute("questions", questions);
    return "rh/candidats-offre";
  }

  /** Vue RH des candidatures en cours : etape en cours et reponses au questionnaire. */
  private List<CandidatureRhVue> buildCandidaturesRh() {
    return buildCandidatures(candidatureService.getCandidaturesEnCours(), null, null);
  }

  private List<CandidatureRhVue> buildCandidatures(List<Candidature> candidatures,
      Questionnaire questionnaireDefinition, List<Question> questions) {
    List<CandidatureRhVue> vues = new ArrayList<>();
    for (Candidature candidature : candidatures) {
      QuestionnaireVue questionnaireVue = QuestionnaireVue.vide();
      if (questionnaireDefinition != null || (candidature.getEtape() != null
          && "QUESTIONNAIRE".equals(candidature.getEtape().getCode()))) {
        Questionnaire q = questionnaireDefinition != null ? questionnaireDefinition
            : questionnaireService
            .getQuestionnaire(candidature.getOffre().getId()).orElse(null);
        if (q != null) {
          List<Question> questionsPourCandidature = questions != null ? questions
              : questionnaireService.getQuestions(q.getId());
          questionnaireVue = QuestionnaireVue.de(q, questionsPourCandidature,
              questionnaireService.getReponses(candidature.getId()));
        }
      }
      vues.add(new CandidatureRhVue(candidature, questionnaireVue));
    }
    return vues;
  }

  /** Le RH valide l'etape en cours : la candidature passe a l'etape suivante. */
  @PostMapping("/candidature/{id}/valider")
  public String validerCandidature(@PathVariable Integer id, HttpSession session,
      RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      candidatureService.validerEtapeSuivante(id, u);
      ra.addFlashAttribute("success", "Etape validee : la candidature passe a l'etape suivante.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  /** Le RH elimine la candidature. */
  @PostMapping("/candidature/{id}/eliminer")
  public String eliminerCandidature(@PathVariable Integer id, HttpSession session,
      RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      candidatureService.eliminer(id, u);
      ra.addFlashAttribute("success", "Candidature eliminee.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/candidature/{id}/entretien")
  public String noterEntretien(@PathVariable Integer id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateEntretien,
      @RequestParam String lieu,
      @RequestParam BigDecimal noteHonneteteCv,
      @RequestParam BigDecimal notePersonnalite,
      @RequestParam BigDecimal noteCompetences,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      candidatureService.enregistrerEntretien(id, u, dateEntretien, lieu, noteHonneteteCv,
          notePersonnalite, noteCompetences);
      ra.addFlashAttribute("success", "Evaluation de l'entretien enregistrée.");
    } catch (Exception e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/questionnaire")
  public String creerQuestionnaire(@RequestParam Integer idOffre,
      @RequestParam String titre,
      @RequestParam(required = false) BigDecimal seuilReussite,
      @RequestParam List<String> enonce,
      @RequestParam(required = false) List<BigDecimal> points,
      @RequestParam(required = false) List<String> type,
      @RequestParam(required = false) List<String> choix,
      @RequestParam(required = false) List<String> bonneReponse,
      HttpSession session, RedirectAttributes ra) {
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try {
      List<Integer> indicesCorrects = new ArrayList<>();
      if (bonneReponse != null) {
        for (String valeur : bonneReponse) {
          indicesCorrects.add(valeur == null || valeur.isBlank() ? null
              : Integer.valueOf(valeur));
        }
      }
      questionnaireService.creerQuestionnaire(idOffre, u, titre, seuilReussite,
          enonce, points, type, choix, indicesCorrects);
      ra.addFlashAttribute("success", "Questionnaire créé pour cette offre.");
    } catch (RuntimeException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
  }

  @PostMapping("/offre/{id}/questionnaire")
  public String modifierQuestionnaire(@PathVariable Integer id,
      @RequestParam String titre,
      @RequestParam(required = false) BigDecimal seuilReussite,
      @RequestParam List<String> enonce,
      @RequestParam(required = false) List<BigDecimal> points,
      @RequestParam(required = false) List<String> type,
      @RequestParam(required = false) List<String> choix,
      @RequestParam(required = false) List<String> bonneReponse,
      RedirectAttributes ra) {
    try {
      List<Integer> indicesCorrects = new ArrayList<>();
      if (bonneReponse != null) {
        for (String valeur : bonneReponse) {
          indicesCorrects.add(valeur == null || valeur.isBlank() ? null
              : Integer.valueOf(valeur));
        }
      }
      questionnaireService.modifierQuestionnaire(id, titre, seuilReussite, enonce, points,
          type, choix, indicesCorrects);
      ra.addFlashAttribute("success", "Questionnaire modifié et renvoyé aux candidats.");
    } catch (RuntimeException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/offre/" + id + "/candidats";
  }

  @PostMapping("/candidature/{id}/questionnaire/noter")
  public String noterQuestionnaire(@PathVariable Integer id,
      @RequestParam Map<String, String> params,
      RedirectAttributes ra) {
    Map<Integer, BigDecimal> notes = new HashMap<>();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (!entry.getKey().startsWith("note_") || entry.getValue().isBlank()) {
        continue;
      }
      try {
        notes.put(Integer.parseInt(entry.getKey().substring(5)),
            new BigDecimal(entry.getValue()));
      } catch (NumberFormatException e) {
        ra.addFlashAttribute("error", "Une note du questionnaire est invalide.");
        return "redirect:/rh/dashboard";
      }
    }
    try {
      BigDecimal score = questionnaireService.noterReponses(id, notes);
      ra.addFlashAttribute("success", "Questionnaire noté. Score obtenu : " + score + ".");
    } catch (RuntimeException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/rh/dashboard";
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
}
