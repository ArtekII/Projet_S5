package com.project.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_administratif", indexes = @Index(name = "idx_document_utilisateur", columnList = "id_utilisateur"))
public class DocumentAdministratif {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
    @Column(nullable = false, length = 30)
    private String type;
    @Column(nullable = false)
    private String nomFichier;
    @Column(nullable = false, length = 100)
    private String typeMime;
    @JdbcTypeCode(SqlTypes.VARBINARY) @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenu;
    private LocalDate dateDelivrance;
    private LocalDate dateExpiration;
    @Column(nullable = false)
    private LocalDateTime deposeLe;
    @ManyToOne(optional = false) @JoinColumn(name = "id_deposant", nullable = false)
    private Utilisateur deposant;

    public Integer getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur v) { utilisateur = v; }
    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String v) { nomFichier = v; }
    public String getTypeMime() { return typeMime; }
    public void setTypeMime(String v) { typeMime = v; }
    public byte[] getContenu() { return contenu; }
    public void setContenu(byte[] v) { contenu = v; }
    public LocalDate getDateDelivrance() { return dateDelivrance; }
    public void setDateDelivrance(LocalDate v) { dateDelivrance = v; }
    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate v) { dateExpiration = v; }
    public LocalDateTime getDeposeLe() { return deposeLe; }
    public void setDeposeLe(LocalDateTime v) { deposeLe = v; }
    public Utilisateur getDeposant() { return deposant; }
    public void setDeposant(Utilisateur v) { deposant = v; }
}
