package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_dossier", indexes = @Index(name = "idx_journal_utilisateur", columnList = "id_utilisateur,id"))
public class JournalDossier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
    @ManyToOne(optional = false) @JoinColumn(name = "id_acteur", nullable = false)
    private Utilisateur acteur;
    @Column(nullable = false, length = 40)
    private String action;
    @Column(nullable = false, length = 1000)
    private String detail;
    @Column(nullable = false)
    private LocalDateTime dateAction;

    public Long getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur v) { utilisateur = v; }
    public Utilisateur getActeur() { return acteur; }
    public void setActeur(Utilisateur v) { acteur = v; }
    public String getAction() { return action; }
    public void setAction(String v) { action = v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { detail = v; }
    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime v) { dateAction = v; }
}
