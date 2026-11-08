package com.project.service;

import com.project.dto.AnomaliePointage;
import com.project.entity.Planning;
import com.project.entity.Pointage;
import java.time.*;
import java.util.*;
import static com.project.dto.AnomaliePointage.*;

/** Calcul en secondes, avec union des connexions et deduction des pauses recouvertes. */
public final class CalculPointage {
    private CalculPointage() { }
    public record Creneau(LocalDateTime debut, LocalDateTime fin, LocalDateTime pauseDebut, LocalDateTime pauseFin) { }
    private record Intervalle(LocalDateTime debut, LocalDateTime fin) { }
    public record Resultat(long prevu, long travail, long retard, long supplementaires, long absence,
            LocalDateTime premiereEntree, LocalDateTime derniereSortie, boolean enCours, Set<AnomaliePointage> anomalies) { }

    public static Creneau creneau(LocalDate date, Planning planning) {
        if (planning == null || !planning.isJourTravaille()) return null;
        LocalDateTime debut = date.atTime(planning.getHeureDebut());
        LocalDateTime fin = date.atTime(planning.getHeureFin());
        if (!fin.isAfter(debut)) fin = fin.plusDays(1);
        LocalDateTime pauseDebut = null, pauseFin = null;
        if (planning.getPauseDebut() != null) {
            pauseDebut = date.atTime(planning.getPauseDebut());
            if (pauseDebut.isBefore(debut)) pauseDebut = pauseDebut.plusDays(1);
            pauseFin = pauseDebut.toLocalDate().atTime(planning.getPauseFin());
            if (!pauseFin.isAfter(pauseDebut)) pauseFin = pauseFin.plusDays(1);
        }
        return new Creneau(debut, fin, pauseDebut, pauseFin);
    }

    private static long secondes(LocalDateTime debut, LocalDateTime fin) {
        return Math.max(0, Duration.between(debut, fin).getSeconds());
    }

    private static long recouvrement(LocalDateTime a, LocalDateTime b, LocalDateTime c, LocalDateTime d) {
        if (c == null || d == null) return 0;
        return secondes(a.isAfter(c) ? a : c, b.isBefore(d) ? b : d);
    }

    public static Resultat calculer(LocalDate date, Planning planning, List<Pointage> sessions, LocalDateTime maintenant) {
        Creneau horaire = creneau(date, planning);
        long prevu = horaire == null ? 0 : secondes(horaire.debut(), horaire.fin())
                - (horaire.pauseDebut() == null ? 0 : secondes(horaire.pauseDebut(), horaire.pauseFin()));
        Set<AnomaliePointage> anomalies = EnumSet.noneOf(AnomaliePointage.class);
        List<Intervalle> intervalles = new ArrayList<>();
        LocalDateTime premiere = null, derniere = null;
        boolean enCours = false, sortieManquante = false;
        for (Pointage pointage : sessions) {
            LocalDateTime entree = pointage.getEntreeEffective(), sortie = pointage.getSortieEffective();
            if (premiere == null || entree.isBefore(premiere)) premiere = entree;
            if (sortie != null && (derniere == null || sortie.isAfter(derniere))) derniere = sortie;
            if (sortie == null) {
                boolean expiree = (!pointage.isRegularise() && pointage.isSessionExpiree())
                        || maintenant.isAfter(entree.plusHours(24))
                        || (horaire != null && maintenant.isAfter(horaire.fin().plusHours(4)));
                if (expiree) { sortieManquante = true; continue; }
                enCours = true; sortie = maintenant;
            }
            if (sortie.isBefore(entree) || entree.isAfter(maintenant) || sortie.isAfter(maintenant)
                    || Duration.between(entree, sortie).compareTo(Duration.ofHours(24)) > 0) {
                anomalies.add(HORAIRES_INCOHERENTS); continue;
            }
            intervalles.add(new Intervalle(entree, sortie));
        }
        intervalles.sort(Comparator.comparing(Intervalle::debut));
        List<Intervalle> reunis = new ArrayList<>();
        for (Intervalle suivant : intervalles) {
            if (reunis.isEmpty() || suivant.debut().isAfter(reunis.getLast().fin())) { reunis.add(suivant); continue; }
            Intervalle dernier = reunis.removeLast();
            if (suivant.debut().isBefore(dernier.fin())) anomalies.add(SESSIONS_SIMULTANEES);
            reunis.add(new Intervalle(dernier.debut(), dernier.fin().isAfter(suivant.fin()) ? dernier.fin() : suivant.fin()));
        }
        long travail = 0, dansPlanning = 0;
        for (Intervalle intervalle : reunis) {
            travail += secondes(intervalle.debut(), intervalle.fin());
            if (horaire != null) {
                travail -= recouvrement(intervalle.debut(), intervalle.fin(), horaire.pauseDebut(), horaire.pauseFin());
                LocalDateTime debut = intervalle.debut().isAfter(horaire.debut()) ? intervalle.debut() : horaire.debut();
                LocalDateTime fin = intervalle.fin().isBefore(horaire.fin()) ? intervalle.fin() : horaire.fin();
                dansPlanning += secondes(debut, fin) - recouvrement(debut, fin, horaire.pauseDebut(), horaire.pauseFin());
            }
        }
        long retard = horaire == null || premiere == null ? 0 : secondes(horaire.debut(), premiere);
        long supplementaires = planning == null ? 0 : Math.max(0, travail - prevu);
        boolean termine = horaire != null && !maintenant.isBefore(horaire.fin());
        long absence = termine ? Math.max(0, prevu - dansPlanning) : 0;
        if (planning == null && !sessions.isEmpty()) anomalies.add(PLANNING_MANQUANT);
        if (planning != null && !planning.isJourTravaille() && !sessions.isEmpty()) anomalies.add(HORS_PLANNING);
        if (sortieManquante) anomalies.add(SORTIE_MANQUANTE);
        if (retard > 0) anomalies.add(RETARD);
        if (supplementaires > 0) anomalies.add(HEURES_SUP);
        if (termine && sessions.isEmpty()) anomalies.add(ABSENCE);
        if (termine && !enCours && !sortieManquante && derniere != null && derniere.isBefore(horaire.fin())) anomalies.add(DEPART_ANTICIPE);
        return new Resultat(prevu, Math.max(0, travail), retard, supplementaires, absence, premiere, derniere,
                enCours, Collections.unmodifiableSet(anomalies));
    }
}
