package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidats_selec2", uniqueConstraints = @UniqueConstraint(columnNames = "id_candidat_selec1"))
public class CandidatSelec2 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne @JoinColumn(name = "id_candidat_selec1", nullable = false)
    private CandidatSelec1 candidatSelec1;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal points = BigDecimal.ZERO;
    @Column(name = "date_selection")
    private LocalDateTime dateSelection;
    public Integer getId() { return id; }
    public CandidatSelec1 getCandidatSelec1() { return candidatSelec1; }
    public void setCandidatSelec1(CandidatSelec1 v) { candidatSelec1 = v; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal v) { points = v; }
    public LocalDateTime getDateSelection() { return dateSelection; }
    public void setDateSelection(LocalDateTime v) { dateSelection = v; }
}
