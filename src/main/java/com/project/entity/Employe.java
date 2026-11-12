package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employe")
public class Employe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_employe")
    private Integer id;
    @Column(name = "id_utilisateur", nullable = false, unique = true)
    private Integer utilisateurId;
    @Column(name = "id_cv_candidat", nullable = false)
    private Integer cvCandidatId;
    @Column(name = "id_contrat_actuel", nullable = false, unique = true)
    private Integer contratId;
    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Integer utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public Integer getCvCandidatId() {
        return cvCandidatId;
    }

    public void setCvCandidatId(Integer cvCandidatId) {
        this.cvCandidatId = cvCandidatId;
    }

    public Integer getContratId() {
        return contratId;
    }

    public void setContratId(Integer contratId) {
        this.contratId = contratId;
    }

    public LocalDate getDateEmbauche() {
        return dateEmbauche;
    }

    public void setDateEmbauche(LocalDate dateEmbauche) {
        this.dateEmbauche = dateEmbauche;
    }
}
