
package com.project.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Questionnaire de l'etape "questionnaire" d'une offre (1 par offre). */
@Entity
@Table(name = "questionnaire")
public class Questionnaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(name = "seuil_reussite", precision = 6, scale = 2, nullable = false)
    private BigDecimal seuilReussite = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_createur", nullable = false)
    private Utilisateur createur;

    public Questionnaire() {
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

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public BigDecimal getSeuilReussite() {
        return seuilReussite;
    }

    public void setSeuilReussite(BigDecimal seuilReussite) {
        this.seuilReussite = seuilReussite;
    }

    public Utilisateur getCreateur() {
        return createur;
    }

    public void setCreateur(Utilisateur createur) {
        this.createur = createur;
    }
}
