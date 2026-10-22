
package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidature_etape")
public class CandidatureEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidature", nullable = false)
    private Candidature candidature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etape", nullable = false)
    private EtapeRecrutement etape;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_resultat", nullable = false)
    private Resultat resultat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_decideur")
    private Utilisateur decideur;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CandidatureEtape() {
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

    public Candidature getCandidature() {
        return candidature;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public EtapeRecrutement getEtape() {
        return etape;
    }

    public void setEtape(EtapeRecrutement etape) {
        this.etape = etape;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public void setResultat(Resultat resultat) {
        this.resultat = resultat;
    }

    public Utilisateur getDecideur() {
        return decideur;
    }

    public void setDecideur(Utilisateur decideur) {
        this.decideur = decideur;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
