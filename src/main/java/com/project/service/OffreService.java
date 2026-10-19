package com.project.service;

import com.project.entity.*;
import com.project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OffreService {
  private final OffreEmploiRepository offreRepo;
  private final PosteRepository posteRepo;
  private final StatutOffreRepository statutRepo;
  private final DecisionRepository decisionRepo;
  private final RoleRepository roleRepo;
  private final OffreValidationRepository validationRepo;
  private final DemandePosteRepository demandeRepo;
  private final DepartementRepository departementRepo;

  public OffreService(OffreEmploiRepository o, PosteRepository p, StatutOffreRepository s, DecisionRepository d,
      RoleRepository r, OffreValidationRepository v, DemandePosteRepository dp, DepartementRepository dept) {
    this.offreRepo = o;
    this.posteRepo = p;
    this.statutRepo = s;
    this.decisionRepo = d;
    this.roleRepo = r;
    this.validationRepo = v;
    this.demandeRepo = dp;
    this.departementRepo = dept;
  }

  public List<Poste> postesActifs() {
    return posteRepo.findByActifTrueOrderByIntituleAsc();
  }

  public List<Departement> departements() {
    return departementRepo.findAll();
  }

  // Manager cree une offre (poste existant)
  @Transactional
  public OffreEmploi creerOffre(Integer posteId, int anneesExp, int nbPlaces, Utilisateur manager) {
    Poste poste = posteRepo.findById(posteId).orElseThrow();
    Departement dept = manager.getDepartement();
    StatutOffre statut = statutRepo.findByCode("EN_ATTENTE_RH").orElseThrow();
    OffreEmploi o = new OffreEmploi();
    o.setPoste(poste);
    o.setDepartement(dept);
    o.setManager(manager);
    o.setAnneesExperience(anneesExp);
    o.setNbPlaces(nbPlaces);
    o.setStatut(statut);
    o.setCreatedAt(LocalDateTime.now());
    return offreRepo.save(o);
  }

  // Manager demande nouveau poste
  @Transactional
  public DemandePoste demanderNouveauPoste(String intitule, String justification, Utilisateur manager) {
    Decision enAttente = decisionRepo.findByCode("EN_ATTENTE").orElseThrow();
    DemandePoste d = new DemandePoste();
    d.setManager(manager);
    d.setDepartement(manager.getDepartement());
    d.setIntitulePropose(intitule);
    d.setJustification(justification);
    d.setDecision(enAttente);
    d.setCreatedAt(LocalDateTime.now());
    return demandeRepo.save(d);
  }

  // RH valide/refuse demande de poste
  @Transactional
  public DemandePoste traiterDemandePoste(Integer demandeId, boolean valider, String motif, Utilisateur rh) {
    DemandePoste d = demandeRepo.findById(demandeId).orElseThrow();
    Decision dec = decisionRepo.findByCode(valider ? "VALIDEE" : "REFUSEE").orElseThrow();
    d.setDecision(dec);
    d.setRh(rh);
    d.setDecidedAt(LocalDateTime.now());
    if (!valider) {
      d.setMotifRefus(motif);
    } else {
      Poste p = new Poste();
      p.setIntitule(d.getIntitulePropose());
      p.setDescription(d.getJustification());
      p.setActif(true);
      p.setCreatedAt(LocalDateTime.now());
      p = posteRepo.save(p);
      d.setPoste(p);
    }
    return demandeRepo.save(d);
  }

  // RH valide une offre : il revoit le meme formulaire que le manager
  // (poste/exp/places)
  // + un champ salaire a definir -> passe en EN_ATTENTE_FINANCE
  @Transactional
  public OffreEmploi rhValiderOffre(Integer offreId, Integer posteId, Integer anneesExp, Integer nbPlaces,
      BigDecimal salaire, Utilisateur rh, boolean valider, String commentaire) {
    OffreEmploi o = offreRepo.findById(offreId).orElseThrow();
    if (!"EN_ATTENTE_RH".equals(o.getStatut().getCode()))
      throw new IllegalStateException("Offre pas en attente RH");
    Decision dec = decisionRepo.findByCode(valider ? "VALIDEE" : "REFUSEE").orElseThrow();
    Role roleRh = roleRepo.findByCode("RH").orElseThrow();
    BigDecimal avant = o.getSalaire();
    if (valider) {
      if (posteId != null)
        o.setPoste(posteRepo.findById(posteId).orElseThrow());
      if (anneesExp != null && anneesExp >= 0)
        o.setAnneesExperience(anneesExp);
      if (nbPlaces != null && nbPlaces > 0)
        o.setNbPlaces(nbPlaces);
      o.setSalaire(salaire);
      o.setStatut(statutRepo.findByCode("EN_ATTENTE_FINANCE").orElseThrow());
    } else {
      o.setStatut(statutRepo.findByCode("REFUSEE_RH").orElseThrow());
    }
    offreRepo.save(o);
    OffreValidation v = new OffreValidation();
    v.setOffre(o);
    v.setRoleValideur(roleRh);
    v.setValideur(rh);
    v.setDecision(dec);
    v.setSalaireAvant(avant);
    v.setSalaireApres(valider ? salaire : avant);
    v.setCommentaire(commentaire);
    v.setDecidedAt(LocalDateTime.now());
    validationRepo.save(v);
    return o;
  }

  // Finance valide/modifie salaire -> passe en EN_ATTENTE_DIRECTION
  @Transactional
  public OffreEmploi financeValiderOffre(Integer offreId, BigDecimal nouveauSalaire, Utilisateur finance,
      boolean valider, String commentaire) {
    OffreEmploi o = offreRepo.findById(offreId).orElseThrow();
    if (!"EN_ATTENTE_FINANCE".equals(o.getStatut().getCode()))
      throw new IllegalStateException("Offre pas en attente Finance");
    Decision dec = decisionRepo.findByCode(valider ? "VALIDEE" : "REFUSEE").orElseThrow();
    Role roleF = roleRepo.findByCode("FINANCE").orElseThrow();
    BigDecimal avant = o.getSalaire();
    if (valider) {
      if (nouveauSalaire != null)
        o.setSalaire(nouveauSalaire);
      o.setStatut(statutRepo.findByCode("EN_ATTENTE_DIRECTION").orElseThrow());
    } else {
      o.setStatut(statutRepo.findByCode("REFUSEE_FINANCE").orElseThrow());
    }
    offreRepo.save(o);
    OffreValidation v = new OffreValidation();
    v.setOffre(o);
    v.setRoleValideur(roleF);
    v.setValideur(finance);
    v.setDecision(dec);
    v.setSalaireAvant(avant);
    v.setSalaireApres(o.getSalaire());
    v.setCommentaire(commentaire);
    v.setDecidedAt(LocalDateTime.now());
    validationRepo.save(v);
    return o;
  }

  // Direction validation finale -> PUBLIE ou REFUSEE_DIRECTION
  @Transactional
  public OffreEmploi directionValiderOffre(Integer offreId, Utilisateur direction, boolean valider,
      String commentaire) {
    OffreEmploi o = offreRepo.findById(offreId).orElseThrow();
    if (!"EN_ATTENTE_DIRECTION".equals(o.getStatut().getCode()))
      throw new IllegalStateException("Offre pas en attente Direction");
    Decision dec = decisionRepo.findByCode(valider ? "VALIDEE" : "REFUSEE").orElseThrow();
    Role roleD = roleRepo.findByCode("DIRECTION").orElseThrow();
    if (valider) {
      o.setStatut(statutRepo.findByCode("PUBLIEE").orElseThrow());
      o.setDatePublication(LocalDateTime.now());
    } else {
      o.setStatut(statutRepo.findByCode("REFUSEE_DIRECTION").orElseThrow());
    }
    offreRepo.save(o);
    OffreValidation v = new OffreValidation();
    v.setOffre(o);
    v.setRoleValideur(roleD);
    v.setValideur(direction);
    v.setDecision(dec);
    v.setSalaireAvant(o.getSalaire());
    v.setSalaireApres(o.getSalaire());
    v.setCommentaire(commentaire);
    v.setDecidedAt(LocalDateTime.now());
    validationRepo.save(v);
    return o;
  }

  public List<OffreEmploi> offresEnAttenteRh() {
    return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("EN_ATTENTE_RH");
  }

  public List<OffreEmploi> offresEnAttenteFinance() {
    return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("EN_ATTENTE_FINANCE");
  }

  public List<OffreEmploi> offresEnAttenteDirection() {
    return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("EN_ATTENTE_DIRECTION");
  }

  public List<OffreEmploi> offresPubliees() {
    return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("PUBLIEE");
  }

  public List<DemandePoste> demandesEnAttente() {
    return demandeRepo.findByDecision_CodeOrderByCreatedAtDesc("EN_ATTENTE");
  }

  public List<DemandePoste> toutesDemandes() {
    return demandeRepo.findAllByOrderByCreatedAtDesc();
  }

  public List<OffreEmploi> toutesOffres() {
    return offreRepo.findAllByOrderByCreatedAtDesc();
  }

  public List<OffreEmploi> offresDuManager(Integer managerId) {
    return offreRepo.findByManager_IdOrderByCreatedAtDesc(managerId);
  }

  public List<OffreValidation> validations(Integer offreId) {
    return validationRepo.findByOffre_IdOrderByDecidedAtAsc(offreId);
  }

  public Optional<OffreEmploi> findOffre(Integer id) {
    return offreRepo.findById(id);
  }
}
