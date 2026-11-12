package com.project.entity;

import jakarta.persistence.*;
import java.time.*;
import com.project.web.DureeUtils;

/** Photographie des variables validees par les RH, sans calcul monetaire. */
@Entity
@Table(name = "variable_paie_pointage", uniqueConstraints = @UniqueConstraint(name = "uk_variable_pointage_employe_date",
        columnNames = {"id_employe", "date_pointage"}))
public class VariablePaiePointage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;
    @ManyToOne(optional = false) @JoinColumn(name = "id_planning", nullable = false)
    private Planning planning;
    @Column(name = "date_pointage", nullable = false)
    private LocalDate date;
    @Column(nullable = false)
    private long prevuSecondes, travailSecondes, retardSecondes, supplementairesSecondes, absenceSecondes;
    @ManyToOne(optional = false) @JoinColumn(name = "id_rh", nullable = false)
    private Utilisateur rh;
    @Column(nullable = false)
    private LocalDateTime transmisLe;

    public Long getId() { return id; }
    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe v) { employe = v; }
    public Planning getPlanning() { return planning; }
    public void setPlanning(Planning v) { planning = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { date = v; }
    public long getPrevuSecondes() { return prevuSecondes; }
    public void setPrevuSecondes(long v) { prevuSecondes = v; }
    public long getTravailSecondes() { return travailSecondes; }
    public void setTravailSecondes(long v) { travailSecondes = v; }
    public long getRetardSecondes() { return retardSecondes; }
    public void setRetardSecondes(long v) { retardSecondes = v; }
    public long getSupplementairesSecondes() { return supplementairesSecondes; }
    public void setSupplementairesSecondes(long v) { supplementairesSecondes = v; }
    public long getAbsenceSecondes() { return absenceSecondes; }
    public void setAbsenceSecondes(long v) { absenceSecondes = v; }
    public Utilisateur getRh() { return rh; }
    public void setRh(Utilisateur v) { rh = v; }
    public LocalDateTime getTransmisLe() { return transmisLe; }
    public void setTransmisLe(LocalDateTime v) { transmisLe = v; }
    public String getPrevuFormate() { return DureeUtils.formater(prevuSecondes); }
    public String getTravailFormate() { return DureeUtils.formater(travailSecondes); }
    public String getRetardFormate() { return DureeUtils.formater(retardSecondes); }
    public String getSupplementairesFormate() { return DureeUtils.formater(supplementairesSecondes); }
    public String getAbsenceFormate() { return DureeUtils.formater(absenceSecondes); }
}
