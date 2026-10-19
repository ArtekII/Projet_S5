package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_poste")
public class DemandePoste {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "id_manager")
    private Utilisateur manager;
    @ManyToOne
    @JoinColumn(name = "id_departement")
    private Departement departement;
    @Column(name = "intitule_propose")
    private String intitulePropose;
    private String justification;
    @ManyToOne
    @JoinColumn(name = "id_decision")
    private Decision decision;
    @ManyToOne
    @JoinColumn(name = "id_rh")
    private Utilisateur rh;
    @Column(name = "motif_refus")
    private String motifRefus;
    @ManyToOne
    @JoinColumn(name = "id_poste")
    private Poste poste;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public DemandePoste() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilisateur getManager() {
        return manager;
    }

    public void setManager(Utilisateur u) {
        this.manager = u;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement d) {
        this.departement = d;
    }

    public String getIntitulePropose() {
        return intitulePropose;
    }

    public void setIntitulePropose(String s) {
        this.intitulePropose = s;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String s) {
        this.justification = s;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision d) {
        this.decision = d;
    }

    public Utilisateur getRh() {
        return rh;
    }

    public void setRh(Utilisateur u) {
        this.rh = u;
    }

    public String getMotifRefus() {
        return motifRefus;
    }

    public void setMotifRefus(String s) {
        this.motifRefus = s;
    }

    public Poste getPoste() {
        return poste;
    }

    public void setPoste(Poste p) {
        this.poste = p;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime c) {
        this.createdAt = c;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime d) {
        this.decidedAt = d;
    }
}
