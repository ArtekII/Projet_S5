package com.project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "entretien_qualification")
public class EntretienQualification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;
    @Column(nullable = false)
    private String nom;
    public Integer getId() { return id; }
    public OffreEmploi getOffre() { return offre; }
    public void setOffre(OffreEmploi v) { offre = v; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
}
