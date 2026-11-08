package com.project.entity;

import jakarta.persistence.*;
import java.time.*;

/** Notification interne visible par le titulaire et les RH. */
@Entity
@Table(name = "alerte_pointage", uniqueConstraints = @UniqueConstraint(name = "uk_alerte_pointage_jour_code",
        columnNames = {"id_employe", "date_pointage", "code"}))
public class AlertePointage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;
    @Column(name = "date_pointage", nullable = false)
    private LocalDate date;
    @Column(nullable = false, length = 40)
    private String code;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(nullable = false)
    private LocalDateTime detecteeLe;
    private LocalDateTime resolueLe;

    public Long getId() { return id; }
    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe v) { employe = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { date = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { message = v; }
    public LocalDateTime getDetecteeLe() { return detecteeLe; }
    public void setDetecteeLe(LocalDateTime v) { detecteeLe = v; }
    public LocalDateTime getResolueLe() { return resolueLe; }
    public void setResolueLe(LocalDateTime v) { resolueLe = v; }
}
