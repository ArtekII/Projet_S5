package com.project.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Evaluation RH de l'entretien d'une candidature. */
@Entity
@Table(name = "entretien")
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidature", nullable = false, unique = true)
    private Candidature candidature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rh", nullable = false)
    private Utilisateur rh;

    @Column(name = "date_entretien")
    private LocalDateTime dateEntretien;

    @Column(name = "lieu_entretien", length = 255)
    private String lieu;

    @Column(name = "note_honnetete_cv", nullable = false, precision = 4, scale = 2)
    private BigDecimal noteHonneteteCv;

    @Column(name = "note_personnalite", nullable = false, precision = 4, scale = 2)
    private BigDecimal notePersonnalite;

    @Column(name = "note_competences", nullable = false, precision = 4, scale = 2)
    private BigDecimal noteCompetences;

    public Integer getId() { return id; }
    public Candidature getCandidature() { return candidature; }
    public void setCandidature(Candidature candidature) { this.candidature = candidature; }
    public Utilisateur getRh() { return rh; }
    public void setRh(Utilisateur rh) { this.rh = rh; }
    public LocalDateTime getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDateTime dateEntretien) { this.dateEntretien = dateEntretien; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public BigDecimal getNoteHonneteteCv() { return noteHonneteteCv; }
    public void setNoteHonneteteCv(BigDecimal value) { this.noteHonneteteCv = value; }
    public BigDecimal getNotePersonnalite() { return notePersonnalite; }
    public void setNotePersonnalite(BigDecimal value) { this.notePersonnalite = value; }
    public BigDecimal getNoteCompetences() { return noteCompetences; }
    public void setNoteCompetences(BigDecimal value) { this.noteCompetences = value; }

    public BigDecimal getNoteTotale() {
        return noteHonneteteCv.add(notePersonnalite).add(noteCompetences);
    }
}
