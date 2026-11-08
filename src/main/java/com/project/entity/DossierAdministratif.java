package com.project.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "dossier_administratif")
public class DossierAdministratif {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne(optional = false) @JoinColumn(name = "id_utilisateur", nullable = false, unique = true)
    private Utilisateur utilisateur;
    @Version
    private Long revision;
    @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) @Column(length = 100)
    private String nom;
    @NotBlank(message = "Le prénom est obligatoire.") @Size(max = 100) @Column(length = 100)
    private String prenom;
    @NotNull(message = "La date de naissance est obligatoire.") @Past(message = "La date de naissance doit être passée.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateNaissance;
    @Size(max = 150) @Column(length = 150)
    private String lieuNaissance;
    @Size(max = 100) @Column(length = 100)
    private String nationalite;
    @Size(max = 100) @Column(length = 100)
    private String numeroIdentite;
    @NotBlank(message = "L'adresse est obligatoire.") @Size(max = 500) @Column(length = 500)
    private String adresse;
    @NotBlank(message = "Le téléphone est obligatoire.") @Size(max = 40) @Column(length = 40)
    private String telephone;
    @NotBlank(message = "L'email de contact est obligatoire.") @Email(message = "L'email de contact est invalide.") @Size(max = 255)
    private String emailPersonnel;
    @NotBlank(message = "La situation matrimoniale est obligatoire.")
    @Pattern(regexp = "CELIBATAIRE|MARIE|DIVORCE|VEUF|UNION_LIBRE", message = "Choisissez une situation matrimoniale valide.") @Column(length = 20)
    private String situationMatrimoniale;
    @Min(0) @Max(50)
    private Integer nombreEnfants;
    @Size(max = 150) @Column(length = 150)
    private String contactUrgence;
    @Size(max = 40) @Column(length = 40)
    private String telephoneUrgence;
    @Column(nullable = false)
    private boolean signature, badge, compte, materiel, formation, paie;
    private LocalDateTime modifieLe;

    public Integer getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur v) { utilisateur = v; }
    public Long getRevision() { return revision; }
    public void setRevision(Long v) { revision = v; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String v) { prenom = v; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate v) { dateNaissance = v; }
    public String getLieuNaissance() { return lieuNaissance; }
    public void setLieuNaissance(String v) { lieuNaissance = v; }
    public String getNationalite() { return nationalite; }
    public void setNationalite(String v) { nationalite = v; }
    public String getNumeroIdentite() { return numeroIdentite; }
    public void setNumeroIdentite(String v) { numeroIdentite = v; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { adresse = v; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { telephone = v; }
    public String getEmailPersonnel() { return emailPersonnel; }
    public void setEmailPersonnel(String v) { emailPersonnel = v; }
    public String getSituationMatrimoniale() { return situationMatrimoniale; }
    public void setSituationMatrimoniale(String v) { situationMatrimoniale = v; }
    public Integer getNombreEnfants() { return nombreEnfants; }
    public void setNombreEnfants(Integer v) { nombreEnfants = v; }
    public String getContactUrgence() { return contactUrgence; }
    public void setContactUrgence(String v) { contactUrgence = v; }
    public String getTelephoneUrgence() { return telephoneUrgence; }
    public void setTelephoneUrgence(String v) { telephoneUrgence = v; }
    public boolean isSignature() { return signature; }
    public void setSignature(boolean v) { signature = v; }
    public boolean isBadge() { return badge; }
    public void setBadge(boolean v) { badge = v; }
    public boolean isCompte() { return compte; }
    public void setCompte(boolean v) { compte = v; }
    public boolean isMateriel() { return materiel; }
    public void setMateriel(boolean v) { materiel = v; }
    public boolean isFormation() { return formation; }
    public void setFormation(boolean v) { formation = v; }
    public boolean isPaie() { return paie; }
    public void setPaie(boolean v) { paie = v; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime v) { modifieLe = v; }
}
