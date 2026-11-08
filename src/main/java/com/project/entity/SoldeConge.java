package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Solde d'un employe pour un type de conge et une periode d'acquisition.
 * La colonne PostgreSQL {@code solde_disponible} est generee par la base ;
 * la valeur est aussi exposee par {@link #getSoldeDisponible()} pour les
 * environnements de test qui ne creent pas cette colonne.
 */
@Entity
@Table(name = "solde_conge",
        uniqueConstraints = @UniqueConstraint(name = "uk_solde_conge",
                columnNames = {"id_employe", "id_type_conge", "periode_debut"}))
public class SoldeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solde")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(name = "jours_acquis", nullable = false, precision = 5, scale = 2)
    private BigDecimal joursAcquis = BigDecimal.ZERO;

    @Column(name = "jours_pris", nullable = false, precision = 5, scale = 2)
    private BigDecimal joursPris = BigDecimal.ZERO;

    @Column(name = "date_maj", nullable = false)
    private LocalDateTime dateMaj;

    @PrePersist
    @PreUpdate
    private void avantEnregistrement() {
        if (joursAcquis == null) joursAcquis = BigDecimal.ZERO;
        if (joursPris == null) joursPris = BigDecimal.ZERO;
        if (dateMaj == null) dateMaj = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe employe) { this.employe = employe; }

    public TypeConge getTypeConge() { return typeConge; }
    public void setTypeConge(TypeConge typeConge) { this.typeConge = typeConge; }

    public LocalDate getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(LocalDate periodeDebut) { this.periodeDebut = periodeDebut; }

    public LocalDate getPeriodeFin() { return periodeFin; }
    public void setPeriodeFin(LocalDate periodeFin) { this.periodeFin = periodeFin; }

    public BigDecimal getJoursAcquis() { return joursAcquis; }
    public void setJoursAcquis(BigDecimal joursAcquis) { this.joursAcquis = joursAcquis; }

    public BigDecimal getJoursPris() { return joursPris; }
    public void setJoursPris(BigDecimal joursPris) { this.joursPris = joursPris; }

    public LocalDateTime getDateMaj() { return dateMaj; }
    public void setDateMaj(LocalDateTime dateMaj) { this.dateMaj = dateMaj; }

    @Transient
    public BigDecimal getSoldeDisponible() {
        BigDecimal acquis = joursAcquis == null ? BigDecimal.ZERO : joursAcquis;
        BigDecimal pris = joursPris == null ? BigDecimal.ZERO : joursPris;
        return acquis.subtract(pris);
    }
}
