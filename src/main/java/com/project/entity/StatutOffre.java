package com.project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "statut_offre")
public class StatutOffre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String code;
    private String libelle;

    public StatutOffre() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String c) {
        this.code = c;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String l) {
        this.libelle = l;
    }
}
