package com.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "candidats_selec3", uniqueConstraints = @UniqueConstraint(columnNames = "id_candidat_selec2"))
public class CandidatSelec3 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne @JoinColumn(name = "id_candidat_selec2", nullable = false)
    private CandidatSelec2 candidatSelec2;
    @Column(name = "note_finale", precision = 5, scale = 2)
    private BigDecimal noteFinale;
    private Integer rang;
    public Integer getId() { return id; }
    public CandidatSelec2 getCandidatSelec2() { return candidatSelec2; }
    public void setCandidatSelec2(CandidatSelec2 v) { candidatSelec2 = v; }
    public BigDecimal getNoteFinale() { return noteFinale; }
    public void setNoteFinale(BigDecimal v) { noteFinale = v; }
    public Integer getRang() { return rang; }
    public void setRang(Integer v) { rang = v; }
}
