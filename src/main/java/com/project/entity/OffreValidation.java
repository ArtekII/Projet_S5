package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offre_validation")
public class OffreValidation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "id_offre")
    private OffreEmploi offre;
    @ManyToOne
    @JoinColumn(name = "id_role_valideur")
    private Role roleValideur;
    @ManyToOne
    @JoinColumn(name = "id_valideur")
    private Utilisateur valideur;
    @ManyToOne
    @JoinColumn(name = "id_decision")
    private Decision decision;
    @Column(name = "salaire_avant")
    private BigDecimal salaireAvant;
    @Column(name = "salaire_apres")
    private BigDecimal salaireApres;
    private String commentaire;
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public OffreValidation() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OffreEmploi getOffre() {
        return offre;
    }

    public void setOffre(OffreEmploi o) {
        this.offre = o;
    }

    public Role getRoleValideur() {
        return roleValideur;
    }

    public void setRoleValideur(Role r) {
        this.roleValideur = r;
    }

    public Utilisateur getValideur() {
        return valideur;
    }

    public void setValideur(Utilisateur u) {
        this.valideur = u;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision d) {
        this.decision = d;
    }

    public BigDecimal getSalaireAvant() {
        return salaireAvant;
    }

    public void setSalaireAvant(BigDecimal s) {
        this.salaireAvant = s;
    }

    public BigDecimal getSalaireApres() {
        return salaireApres;
    }

    public void setSalaireApres(BigDecimal s) {
        this.salaireApres = s;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String c) {
        this.commentaire = c;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime d) {
        this.decidedAt = d;
    }
}
