package com.project.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "correction_pointage")
public class CorrectionPointage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "id_pointage", nullable = false)
    private Pointage pointage;
    @ManyToOne(optional = false) @JoinColumn(name = "id_rh", nullable = false)
    private Utilisateur rh;
    private LocalDateTime ancienneEntree;
    private LocalDateTime ancienneSortie;
    @Column(nullable = false)
    private LocalDateTime nouvelleEntree;
    @Column(nullable = false)
    private LocalDateTime nouvelleSortie;
    @Column(nullable = false, length = 1000)
    private String motif;
    @Column(nullable = false)
    private LocalDateTime corrigeLe;

    public Long getId() { return id; }
    public Pointage getPointage() { return pointage; }
    public void setPointage(Pointage v) { pointage = v; }
    public Utilisateur getRh() { return rh; }
    public void setRh(Utilisateur v) { rh = v; }
    public LocalDateTime getAncienneEntree() { return ancienneEntree; }
    public void setAncienneEntree(LocalDateTime v) { ancienneEntree = v; }
    public LocalDateTime getAncienneSortie() { return ancienneSortie; }
    public void setAncienneSortie(LocalDateTime v) { ancienneSortie = v; }
    public LocalDateTime getNouvelleEntree() { return nouvelleEntree; }
    public void setNouvelleEntree(LocalDateTime v) { nouvelleEntree = v; }
    public LocalDateTime getNouvelleSortie() { return nouvelleSortie; }
    public void setNouvelleSortie(LocalDateTime v) { nouvelleSortie = v; }
    public String getMotif() { return motif; }
    public void setMotif(String v) { motif = v; }
    public LocalDateTime getCorrigeLe() { return corrigeLe; }
    public void setCorrigeLe(LocalDateTime v) { corrigeLe = v; }
}
