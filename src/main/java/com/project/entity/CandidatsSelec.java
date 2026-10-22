
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "candidats_selec",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_candidats_selec",
            columnNames = {
                "id_offre",
                "id_cv_candidat",
                "id_etape"
            }
        )
    }
)
public class CandidatsSelec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offreEmploi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cv_candidat", nullable = false)
    private CvCandidat cvCandidat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etape", nullable = false)
    private Etape etape;

    @Column(name = "note", precision = 5, scale = 2)
    private BigDecimal note = BigDecimal.ZERO;

    @Column(name = "rang")
    private Integer rang;

    @Column(name = "date_selection", nullable = false)
    private LocalDateTime dateSelection;

    public CandidatsSelec() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OffreEmploi getOffreEmploi() {
        return offreEmploi;
    }

    public void setOffreEmploi(OffreEmploi offreEmploi) {
        this.offreEmploi = offreEmploi;
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
