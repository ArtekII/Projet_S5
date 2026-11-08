package com.project.entity;

import jakarta.persistence.*;
import java.time.*;

/** Version immuable des horaires d'un poste pour un jour de la semaine. */
@Entity
@Table(name = "planning", uniqueConstraints = @UniqueConstraint(name = "uk_planning_poste_jour_effet",
        columnNames = {"id_poste", "jour_semaine", "date_effet"}))
public class Planning {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "id_poste", nullable = false)
    private Poste poste;
    @Column(nullable = false)
    private int jourSemaine;
    @Column(nullable = false)
    private LocalDate dateEffet;
    @Column(nullable = false)
    private boolean jourTravaille;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private LocalTime pauseDebut;
    private LocalTime pauseFin;
    @ManyToOne(optional = false) @JoinColumn(name = "id_rh", nullable = false)
    private Utilisateur rh;
    @Column(nullable = false)
    private LocalDateTime creeLe;

    public Integer getId() { return id; }
    public Poste getPoste() { return poste; }
    public void setPoste(Poste v) { poste = v; }
    public int getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(int v) { jourSemaine = v; }
    public LocalDate getDateEffet() { return dateEffet; }
    public void setDateEffet(LocalDate v) { dateEffet = v; }
    public boolean isJourTravaille() { return jourTravaille; }
    public void setJourTravaille(boolean v) { jourTravaille = v; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime v) { heureDebut = v; }
    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime v) { heureFin = v; }
    public LocalTime getPauseDebut() { return pauseDebut; }
    public void setPauseDebut(LocalTime v) { pauseDebut = v; }
    public LocalTime getPauseFin() { return pauseFin; }
    public void setPauseFin(LocalTime v) { pauseFin = v; }
    public Utilisateur getRh() { return rh; }
    public void setRh(Utilisateur v) { rh = v; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime v) { creeLe = v; }
    public String getJourLibelle() { return DayOfWeek.of(jourSemaine).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH); }
}
