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
}
