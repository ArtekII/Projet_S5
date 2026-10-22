
package com.project.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Question d'un questionnaire (QCM ou reponse libre). */
@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_questionnaire", nullable = false)
    private Questionnaire questionnaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type", nullable = false)
    private TypeQuestion type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enonce;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal points = BigDecimal.ONE;

    @Column(nullable = false)
    private Integer ordre = 1;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<ChoixReponse> choix = new ArrayList<>();

    public Question() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    public TypeQuestion getType() {
        return type;
    }

    public void setType(TypeQuestion type) {
        this.type = type;
    }

    public String getEnonce() {
        return enonce;
    }

    public void setEnonce(String enonce) {
        this.enonce = enonce;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public void setOrdre(Integer ordre) {
        this.ordre = ordre;
    }

    public List<ChoixReponse> getChoix() {
        return choix;
    }

    public void setChoix(List<ChoixReponse> choix) {
        this.choix = choix;
    }

    public String getChoixTexte() {
        return choix.stream().map(ChoixReponse::getLibelle)
                .reduce((a, b) -> a + "|" + b).orElse("");
    }

    public Integer getBonneReponseIndex() {
        for (int i = 0; i < choix.size(); i++) {
            if (choix.get(i).isEstCorrect()) {
                return i;
            }
        }
        return null;
    }
}
