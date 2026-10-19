package com.project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String code;
    private String libelle;

    public Role() {
    }

    public Role(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
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
