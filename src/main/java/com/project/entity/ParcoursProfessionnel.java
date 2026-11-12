package com.project.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "parcours_professionnel", indexes = @Index(name = "idx_parcours_utilisateur", columnList = "id_utilisateur"))
public class ParcoursProfessionnel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
    @NotBlank(message = "Le type d'événement est obligatoire.")
    @Pattern(regexp = "AFFECTATION|SALAIRE|EXPERIENCE", message = "Choisissez un type d'événement valide.") @Column(nullable = false, length = 20)
    private String type;
    @NotBlank(message = "L'intitulé de l'événement est obligatoire.") @Size(max = 255) @Column(nullable = false)
    private String libelle;
    @NotNull(message = "La date d'effet est obligatoire.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Column(nullable = false)
    private LocalDate dateDebut;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(precision = 12, scale = 2)
    private BigDecimal montant;
    @Size(max = 2000) @Column(length = 2000)
    private String description;
    @Column(nullable = false)
    private LocalDateTime creeLe;

    public Integer getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur v) { utilisateur = v; }
    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String v) { libelle = v; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate v) { dateDebut = v; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate v) { dateFin = v; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal v) { montant = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime v) { creeLe = v; }
}
