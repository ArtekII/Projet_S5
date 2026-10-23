
package com.project.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Reponse d'un candidat a une question du questionnaire. */
@Entity
@Table(name = "reponse_candidat")
public class ReponseCandidat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidature", nullable = false)
    private Candidature candidature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_question", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_choix")
    private ChoixReponse choix;

    @Column(name = "reponse_texte", columnDefinition = "TEXT")
    private String reponseTexte;

    @Column(name = "points_obtenus", precision = 5, scale = 2)
    private BigDecimal pointsObtenus;

    public ReponseCandidat() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Candidature getCandidature() {
        return candidature;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public ChoixReponse getChoix() {
        return choix;
    }

    public void setChoix(ChoixReponse choix) {
        this.choix = choix;
    }

    public String getReponseTexte() {
        return reponseTexte;
    }

    public void setReponseTexte(String reponseTexte) {
        this.reponseTexte = reponseTexte;
    }

    public BigDecimal getPointsObtenus() {
        return pointsObtenus;
    }

    public void setPointsObtenus(BigDecimal pointsObtenus) {
        this.pointsObtenus = pointsObtenus;
    }


}
