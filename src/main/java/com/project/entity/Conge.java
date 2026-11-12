package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Demande de conge d'un employe. */
@Entity
@Table(name = "conge", indexes = {
        @Index(name = "idx_conge_employe_dates", columnList = "id_employe,date_debut,date_fin"),
        @Index(name = "idx_conge_statut", columnList = "statut")
})
public class Conge {
    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_VALIDE = "VALIDE";
    public static final String STATUT_REFUSE = "REFUSE";
    public static final String STATUT_ANNULE = "ANNULE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conge")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nb_jours", nullable = false, precision = 5, scale = 2)
    private BigDecimal nbJours;

    @Column(nullable = false, length = 20)
    private String statut = STATUT_EN_ATTENTE;

    @Column(length = 500)
    private String commentaire;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @PrePersist
    private void avantCreation() {
        if (statut == null) statut = STATUT_EN_ATTENTE;
        if (dateDemande == null) dateDemande = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe employe) { this.employe = employe; }

    public TypeConge getTypeConge() { return typeConge; }
    public void setTypeConge(TypeConge typeConge) { this.typeConge = typeConge; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getNbJours() { return nbJours; }
    public void setNbJours(BigDecimal nbJours) { this.nbJours = nbJours; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }

    public LocalDateTime getDateDecision() { return dateDecision; }
    public void setDateDecision(LocalDateTime dateDecision) { this.dateDecision = dateDecision; }
}
