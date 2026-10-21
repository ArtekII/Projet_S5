package com.project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "etape", uniqueConstraints = @UniqueConstraint(columnNames = "nom"))
public class Etape {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nom;

    public Integer getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }
}
