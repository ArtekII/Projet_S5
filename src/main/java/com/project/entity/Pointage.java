package com.project.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "pointage", indexes = @Index(name = "idx_pointage_employe_date", columnList = "id_employe,date_pointage"))
public class Pointage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long revision;
    @ManyToOne(optional = false) @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;
    @ManyToOne @JoinColumn(name = "id_planning")
    private Planning planning;
    @ManyToOne @JoinColumn(name = "id_poste")
    private Poste poste;
    @Column(name = "date_pointage", nullable = false)
    private LocalDate date;
    @Column(name = "heure_connexion", nullable = false)
    private LocalDateTime connexion;
    @Column(name = "heure_deconnexion")
    private LocalDateTime deconnexion;
    private LocalDateTime connexionRetenue;
    private LocalDateTime deconnexionRetenue;
    @Column(nullable = false)
    private boolean regularise;
    @Column(nullable = false)
    private boolean sessionExpiree;
    @Column(nullable = false, unique = true, length = 36)
    private String jeton;

    public Long getId() { return id; }
    public Long getRevision() { return revision; }
    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe v) { employe = v; }
    public Planning getPlanning() { return planning; }
    public void setPlanning(Planning v) { planning = v; }
    public Poste getPoste() { return poste; }
    public void setPoste(Poste v) { poste = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { date = v; }
    public LocalDateTime getConnexion() { return connexion; }
    public void setConnexion(LocalDateTime v) { connexion = v; }
    public LocalDateTime getDeconnexion() { return deconnexion; }
    public void setDeconnexion(LocalDateTime v) { deconnexion = v; }
    public LocalDateTime getConnexionRetenue() { return connexionRetenue; }
    public void setConnexionRetenue(LocalDateTime v) { connexionRetenue = v; }
    public LocalDateTime getDeconnexionRetenue() { return deconnexionRetenue; }
    public void setDeconnexionRetenue(LocalDateTime v) { deconnexionRetenue = v; }
    public boolean isRegularise() { return regularise; }
    public void setRegularise(boolean v) { regularise = v; }
    public boolean isSessionExpiree() { return sessionExpiree; }
    public void setSessionExpiree(boolean v) { sessionExpiree = v; }
    public String getJeton() { return jeton; }
    public void setJeton(String v) { jeton = v; }
    public LocalDateTime getEntreeEffective() { return regularise ? connexionRetenue : connexion; }
    public LocalDateTime getSortieEffective() { return regularise ? deconnexionRetenue : deconnexion; }
}
