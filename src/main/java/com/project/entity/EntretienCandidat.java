package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "entretien_candidat", uniqueConstraints = @UniqueConstraint(columnNames = {"id_eq", "id_candidat_selec2"}))
public class EntretienCandidat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne @JoinColumn(name = "id_eq", nullable = false)
    private EntretienQualification entretienQualification;
    @ManyToOne @JoinColumn(name = "id_candidat_selec2", nullable = false)
    private CandidatSelec2 candidatSelec2;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal points = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT")
    private String commentaire;
    public Integer getId() { return id; }
    public EntretienQualification getEntretienQualification() { return entretienQualification; }
    public void setEntretienQualification(EntretienQualification v) { entretienQualification = v; }
    public CandidatSelec2 getCandidatSelec2() { return candidatSelec2; }
    public void setCandidatSelec2(CandidatSelec2 v) { candidatSelec2 = v; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal v) { points = v; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String v) { commentaire = v; }
}
