package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Journal des actions effectuees sur une demande de conge. */
@Entity
@Table(name = "historique_conge", indexes = {
        @Index(name = "idx_hist_conge", columnList = "id_conge,date_action")
})
public class HistoriqueConge {
    public static final String ACTION_CREATION = "CREATION";
    public static final String ACTION_VALIDATION = "VALIDATION";
    public static final String ACTION_REFUS = "REFUS";
    public static final String ACTION_ANNULATION = "ANNULATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_conge", nullable = false)
    private Conge conge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "ancien_statut", length = 20)
    private String ancienStatut;

    @Column(name = "nouveau_statut", length = 20)
    private String nouveauStatut;

    @Column(name = "nb_jours", precision = 5, scale = 2)
    private BigDecimal nbJours;

    @Column(name = "solde_avant", precision = 5, scale = 2)
    private BigDecimal soldeAvant;

    @Column(name = "solde_apres", precision = 5, scale = 2)
    private BigDecimal soldeApres;

    /** Utilisateur a l'origine de l'action : l'employe ou le RH. */
    @ManyToOne
    @JoinColumn(name = "id_auteur")
    private Utilisateur auteur;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @Column(length = 500)
    private String commentaire;

    @PrePersist
    private void avantCreation() {
        if (dateAction == null) dateAction = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Conge getConge() { return conge; }
    public void setConge(Conge conge) { this.conge = conge; }

    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe employe) { this.employe = employe; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getAncienStatut() { return ancienStatut; }
    public void setAncienStatut(String ancienStatut) { this.ancienStatut = ancienStatut; }

    public String getNouveauStatut() { return nouveauStatut; }
    public void setNouveauStatut(String nouveauStatut) { this.nouveauStatut = nouveauStatut; }

    public BigDecimal getNbJours() { return nbJours; }
    public void setNbJours(BigDecimal nbJours) { this.nbJours = nbJours; }

    public BigDecimal getSoldeAvant() { return soldeAvant; }
    public void setSoldeAvant(BigDecimal soldeAvant) { this.soldeAvant = soldeAvant; }

    public BigDecimal getSoldeApres() { return soldeApres; }
    public void setSoldeApres(BigDecimal soldeApres) { this.soldeApres = soldeApres; }

    public Utilisateur getAuteur() { return auteur; }
    public void setAuteur(Utilisateur auteur) { this.auteur = auteur; }

    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}
