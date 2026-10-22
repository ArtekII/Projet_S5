
package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "candidature",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_candidature_offre_candidat",
            columnNames = {"id_offre", "id_candidat"}
        )
    }
)
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidat", nullable = false)
    private Utilisateur candidat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cv", nullable = false)
    private Cv cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etape", nullable = false)
    private EtapeRecrutement etape;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_statut", nullable = false)
    private StatutCandidature statut;

    @Column(name = "score_questionnaire", precision = 6, scale = 2)
    private BigDecimal scoreQuestionnaire;

    @Column(name = "cdi_autorise", nullable = false)
    private boolean cdiAutorise = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rh_cdi")
    private Utilisateur rhCdi;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Candidature() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
    }

    public Utilisateur getCandidat() {
        return candidat;
    }

    public void setCandidat(Utilisateur candidat) {
        this.candidat = candidat;
    }

    public Cv getCv() {
        return cv;
    }

    public void setCv(Cv cv) {
        this.cv = cv;
    }

    public EtapeRecrutement getEtape() {
        return etape;
    }

    public void setEtape(EtapeRecrutement etape) {
        this.etape = etape;
    }

    public StatutCandidature getStatut() {
        return statut;
    }

    public void setStatut(StatutCandidature statut) {
        this.statut = statut;
    }

    public BigDecimal getScoreQuestionnaire() {
        return scoreQuestionnaire;
    }

    public void setScoreQuestionnaire(BigDecimal scoreQuestionnaire) {
        this.scoreQuestionnaire = scoreQuestionnaire;
    }

    public boolean isCdiAutorise() {
        return cdiAutorise;
    }

    public void setCdiAutorise(boolean cdiAutorise) {
        this.cdiAutorise = cdiAutorise;
    }

    public Utilisateur getRhCdi() {
        return rhCdi;
    }

    public void setRhCdi(Utilisateur rhCdi) {
        this.rhCdi = rhCdi;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
