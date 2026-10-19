package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offre_emploi")
public class OffreEmploi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "id_poste")
    private Poste poste;
    @ManyToOne
    @JoinColumn(name = "id_departement")
    private Departement departement;
    @ManyToOne
    @JoinColumn(name = "id_manager")
    private Utilisateur manager;
    @Column(name = "annees_experience")
    private Integer anneesExperience;
    @Column(name = "nb_places")
    private Integer nbPlaces;
    private BigDecimal salaire;
    @ManyToOne
    @JoinColumn(name = "id_statut")
    private StatutOffre statut;
    @Column(name = "date_publication")
    private LocalDateTime datePublication;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public OffreEmploi() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Poste getPoste() {
        return poste;
    }

    public void setPoste(Poste p) {
        this.poste = p;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement d) {
        this.departement = d;
    }

    public Utilisateur getManager() {
        return manager;
    }

    public void setManager(Utilisateur u) {
        this.manager = u;
    }

    public Integer getAnneesExperience() {
        return anneesExperience;
    }

    public void setAnneesExperience(Integer a) {
        this.anneesExperience = a;
    }

    public Integer getNbPlaces() {
        return nbPlaces;
    }

    public void setNbPlaces(Integer n) {
        this.nbPlaces = n;
    }

    public BigDecimal getSalaire() {
        return salaire;
    }

    public void setSalaire(BigDecimal s) {
        this.salaire = s;
    }

    public StatutOffre getStatut() {
        return statut;
    }

    public void setStatut(StatutOffre s) {
        this.statut = s;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime d) {
        this.datePublication = d;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime c) {
        this.createdAt = c;
    }
}
