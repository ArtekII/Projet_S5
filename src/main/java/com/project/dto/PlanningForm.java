package com.project.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import org.springframework.format.annotation.DateTimeFormat;

public class PlanningForm {
    @NotNull(message = "Choisissez un poste.")
    private Integer posteId;
    @Min(1) @Max(7)
    private int jourSemaine = 1;
    @NotNull(message = "La date d'effet est obligatoire.") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateEffet;
    private boolean jourTravaille = true;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime heureDebut, heureFin, pauseDebut, pauseFin;

    public Integer getPosteId() { return posteId; }
    public void setPosteId(Integer v) { posteId = v; }
    public int getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(int v) { jourSemaine = v; }
    public LocalDate getDateEffet() { return dateEffet; }
    public void setDateEffet(LocalDate v) { dateEffet = v; }
    public boolean isJourTravaille() { return jourTravaille; }
    public void setJourTravaille(boolean v) { jourTravaille = v; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime v) { heureDebut = v; }
    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime v) { heureFin = v; }
    public LocalTime getPauseDebut() { return pauseDebut; }
    public void setPauseDebut(LocalTime v) { pauseDebut = v; }
    public LocalTime getPauseFin() { return pauseFin; }
    public void setPauseFin(LocalTime v) { pauseFin = v; }
}
