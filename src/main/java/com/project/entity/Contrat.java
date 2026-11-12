package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrat")
public class Contrat {
    public static final String STATUT_FINALISE = "FINALISE";
    public static final String STATUT_PROGRAMME = "PROGRAMME";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Null pour un contrat successif rattache directement a un employe. */
    @Column(name = "id_candidature")
    private Integer idCandidature;

    /** Employe concerne par le contrat (renseigne des la creation du dossier employe). */
    @Column(name = "id_employe")
    private Integer idEmploye;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_condition", nullable = false)
    private ConditionTravail condition;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_contrat", nullable = false)
    private TypeContrat typeContrat;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    /** Duree du contrat en mois (null pour un CDI ou une duree non renseignee). */
    @Column(name = "duree_mois")
    private Integer dureeMois;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "varchar(20) default 'FINALISE'")
    private String statut = "FINALISE";

    @Column(name = "id_rh_preparation")
    private Integer idRhPreparation;

    public String getStatut() { return statut; }
    public void setStatut(String valeur) { statut = valeur; }
    public Integer getIdRhPreparation() { return idRhPreparation; }
    public void setIdRhPreparation(Integer valeur) { idRhPreparation = valeur; }

    public Contrat() {
    }

    @PrePersist
    private void beforePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (typeContrat == null) {
            typeContrat = new TypeContrat();
            typeContrat.setId(1);
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdCandidature() {
        return idCandidature;
    }

    public void setIdCandidature(Integer idCandidature) {
        this.idCandidature = idCandidature;
    }

    public Integer getIdEmploye() {
        return idEmploye;
    }

    public void setIdEmploye(Integer idEmploye) {
        this.idEmploye = idEmploye;
    }

    public ConditionTravail getCondition() {
        return condition;
    }

    public void setCondition(ConditionTravail condition) {
        this.condition = condition;
    }

    public TypeContrat getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(TypeContrat typeContrat) {
        this.typeContrat = typeContrat;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public Integer getDureeMois() {
        return dureeMois;
    }

    public void setDureeMois(Integer dureeMois) {
        this.dureeMois = dureeMois;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
