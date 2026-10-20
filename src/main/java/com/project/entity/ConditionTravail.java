package com.project.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "condition_travail", uniqueConstraints = @UniqueConstraint(columnNames = { "id_candidature", "version" }))
public class ConditionTravail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_candidature", nullable = false)
    private Integer idCandidature;

    @Column(nullable = false)
    private Integer version = 1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_role_proposant", nullable = false)
    private Role roleProposant;

    @DecimalMin("0.00")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salaire;

    @DecimalMin(value = "0", inclusive = false)
    @Column(name = "heures_par_semaine", nullable = false, precision = 4, scale = 1)
    private BigDecimal heuresParSemaine;

    @Column(length = 100)
    private String horaires;

    @Column(name = "lieu_travail", nullable = false, length = 255)
    private String lieuTravail;

    @ManyToOne
    @JoinColumn(name = "id_reponse")
    private ReponseCondition reponse;

    @Column(columnDefinition = "TEXT")
    private String remarque;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_decision_rh", nullable = false)
    private Decision decisionRh;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ConditionTravail() {
    }

    @PrePersist
    private void beforePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (decisionRh == null) {
            decisionRh = new Decision();
            decisionRh.setId(1);
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdCandidature() {
        return idCandidature;
    }

    public void setIdCandidature(Integer idCandidature) {
        this.idCandidature = idCandidature;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Role getRoleProposant() {
        return roleProposant;
    }

    public void setRoleProposant(Role roleProposant) {
        this.roleProposant = roleProposant;
    }

    public BigDecimal getSalaire() {
        return salaire;
    }

    public void setSalaire(BigDecimal salaire) {
        this.salaire = salaire;
    }

    public BigDecimal getHeuresParSemaine() {
        return heuresParSemaine;
    }

    public void setHeuresParSemaine(BigDecimal heuresParSemaine) {
        this.heuresParSemaine = heuresParSemaine;
    }

    public String getHoraires() {
        return horaires;
    }

    public void setHoraires(String horaires) {
        this.horaires = horaires;
    }

    public String getLieuTravail() {
        return lieuTravail;
    }

    public void setLieuTravail(String lieuTravail) {
        this.lieuTravail = lieuTravail;
    }

    public ReponseCondition getReponse() {
        return reponse;
    }

    public void setReponse(ReponseCondition reponse) {
        this.reponse = reponse;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }

    public Decision getDecisionRh() {
        return decisionRh;
    }

    public void setDecisionRh(Decision decisionRh) {
        this.decisionRh = decisionRh;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
