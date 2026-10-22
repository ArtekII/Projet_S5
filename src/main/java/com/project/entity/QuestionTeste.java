package com.project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "questions_teste")
public class QuestionTeste {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;
    @Column(name = "points_max", nullable = false)
    private Integer pointsMax = 1;
    public Integer getId() { return id; }
    public OffreEmploi getOffre() { return offre; }
    public void setOffre(OffreEmploi v) { offre = v; }
    public String getQuestion() { return question; }
    public void setQuestion(String v) { question = v; }
    public Integer getPointsMax() { return pointsMax; }
    public void setPointsMax(Integer v) { pointsMax = v; }
}
