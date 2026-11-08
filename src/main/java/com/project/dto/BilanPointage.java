package com.project.dto;

import com.project.entity.*;
import com.project.service.CalculPointage;
import com.project.web.DureeUtils;
import java.time.*;
import java.util.*;

public class BilanPointage {
    private final Employe employe;
    private final String email;
    private final LocalDate date;
    private final Planning planning;
    private final List<Pointage> sessions;
    private final CalculPointage.Resultat calcul;
    private final VariablePaiePointage transmission;
    private final boolean transmissible;
    private final List<CorrectionPointage> corrections;

    public BilanPointage(Employe employe, String email, LocalDate date, Planning planning, List<Pointage> sessions,
            CalculPointage.Resultat calcul, VariablePaiePointage transmission, boolean transmissible, List<CorrectionPointage> corrections) {
        this.employe = employe; this.email = email; this.date = date; this.planning = planning; this.sessions = sessions;
        this.calcul = calcul; this.transmission = transmission; this.transmissible = transmissible; this.corrections = corrections;
    }
    public Employe getEmploye() { return employe; }
    public String getEmail() { return email; }
    public LocalDate getDate() { return date; }
    public Planning getPlanning() { return planning; }
    public List<Pointage> getSessions() { return sessions; }
    public List<CorrectionPointage> getCorrections() { return corrections; }
    public VariablePaiePointage getTransmission() { return transmission; }
    public boolean isTransmissible() { return transmissible; }
    public boolean isEnCours() { return calcul.enCours(); }
    public long getPrevuSecondes() { return calcul.prevu(); }
    public long getTravailSecondes() { return calcul.travail(); }
    public long getRetardSecondes() { return calcul.retard(); }
    public long getSupplementairesSecondes() { return calcul.supplementaires(); }
    public long getAbsenceSecondes() { return calcul.absence(); }
    public LocalDateTime getPremiereEntree() { return calcul.premiereEntree(); }
    public LocalDateTime getDerniereSortie() { return calcul.derniereSortie(); }
    public Set<AnomaliePointage> getAnomalies() { return calcul.anomalies(); }
    public String getPrevuFormate() { return DureeUtils.formater(calcul.prevu()); }
    public String getTravailFormate() { return DureeUtils.formater(calcul.travail()); }
    public String getRetardFormate() { return DureeUtils.formater(calcul.retard()); }
    public String getSupplementairesFormate() { return DureeUtils.formater(calcul.supplementaires()); }
    public String getAbsenceFormate() { return DureeUtils.formater(calcul.absence()); }
}
