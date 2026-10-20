package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidats_selec1", uniqueConstraints = @UniqueConstraint(columnNames = {"id_offre", "id_cv_candidat"}))
public class CandidatSelec1 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;
    @ManyToOne @JoinColumn(name = "id_cv_candidat", nullable = false)
    private CvCandidat cvCandidat;
    @Column(name = "date_selection", nullable = false)
    private LocalDateTime dateSelection;
    public Integer getId() { return id; }
    public OffreEmploi getOffre() { return offre; }
    public void setOffre(OffreEmploi v) { offre = v; }
    public CvCandidat getCvCandidat() { return cvCandidat; }
    public void setCvCandidat(CvCandidat v) { cvCandidat = v; }
    public LocalDateTime getDateSelection() { return dateSelection; }
    public void setDateSelection(LocalDateTime v) { dateSelection = v; }
}
