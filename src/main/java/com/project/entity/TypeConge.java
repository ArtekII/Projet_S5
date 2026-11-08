package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Referentiel des types de conges et de leur acquisition mensuelle. */
@Entity
@Table(name = "type_conge")
public class TypeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_conge")
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(name = "est_remunere", nullable = false)
    private boolean remunere = true;

    /** Indique si le type consomme un solde de conges. */
    @Column(name = "decompte_solde", nullable = false)
    private boolean decompteSolde = true;

    /** Jours acquis chaque mois. Null : pas d'acquisition automatique. */
    @Column(name = "acquisition_mensuelle", precision = 4, scale = 2)
    private BigDecimal acquisitionMensuelle;

    @Column(nullable = false)
    private boolean actif = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public boolean isRemunere() { return remunere; }
    public void setRemunere(boolean remunere) { this.remunere = remunere; }

    public boolean isDecompteSolde() { return decompteSolde; }
    public void setDecompteSolde(boolean decompteSolde) { this.decompteSolde = decompteSolde; }

    public BigDecimal getAcquisitionMensuelle() { return acquisitionMensuelle; }
    public void setAcquisitionMensuelle(BigDecimal acquisitionMensuelle) { this.acquisitionMensuelle = acquisitionMensuelle; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
