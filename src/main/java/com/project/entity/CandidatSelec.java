package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidats_selec",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_offre", "id_cv_candidat", "id_etape"}))
public class CandidatSelec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;

    @ManyToOne
    @JoinColumn(name = "id_cv_candidat", nullable = false)
    private CvCandidat cvCandidat;

    @ManyToOne
    @JoinColumn(name = "id_etape", nullable = false)
    private Etape etape;

    @Column(precision = 5, scale = 2)
    private BigDecimal note = BigDecimal.ZERO;

    private Integer rang;

    @Column(name = "date_selection", nullable = false)
    private LocalDateTime dateSelection;

    public Integer getId() {
        return id;
    }

    public OffreEmploi getOffre() {
        return offre;
    }

    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
    }

    public CvCandidat getCvCandidat() {
        return cvCandidat;
    }

    public void setCvCandidat(CvCandidat cvCandidat) {
        this.cvCandidat = cvCandidat;
    }

    public Etape getEtape() {
        return etape;
    }

    public void setEtape(Etape etape) {
        this.etape = etape;
    }

    public BigDecimal getNote() {
        return note;
    }

    public void setNote(BigDecimal note) {
        this.note = note;
    }

    public Integer getRang() {
        return rang;
    }

    public void setRang(Integer rang) {
        this.rang = rang;
    }

    public LocalDateTime getDateSelection() {
        return dateSelection;
    }

    public void setDateSelection(LocalDateTime dateSelection) {
        this.dateSelection = dateSelection;
    }
}
