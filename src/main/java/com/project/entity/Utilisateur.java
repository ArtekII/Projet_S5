package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String email;
    @Column(name = "mot_de_passe")
    private String motDePasse;
    @ManyToOne
    @JoinColumn(name = "id_role")
    private Role role;
    @ManyToOne
    @JoinColumn(name = "id_departement")
    private Departement departement;
    private Boolean actif = true;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Utilisateur() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String e) {
        this.email = e;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String m) {
        this.motDePasse = m;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role r) {
        this.role = r;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement d) {
        this.departement = d;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean a) {
        this.actif = a;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime c) {
        this.createdAt = c;
    }
}
