package com.project.service;

import com.project.entity.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.project.dto.AnomaliePointage.*;
import static org.junit.jupiter.api.Assertions.*;

class CalculPointageTest {
    private final LocalDate date = LocalDate.of(2026, 11, 9);
    private Planning planning(String debut, String fin, String pauseDebut, String pauseFin) {
        Planning planning = new Planning(); planning.setJourTravaille(true);
        planning.setHeureDebut(LocalTime.parse(debut)); planning.setHeureFin(LocalTime.parse(fin));
        if (pauseDebut != null) { planning.setPauseDebut(LocalTime.parse(pauseDebut)); planning.setPauseFin(LocalTime.parse(pauseFin)); }
        return planning;
    }
    private Pointage session(String entree, String sortie) {
        Pointage p = new Pointage(); p.setDate(date); p.setConnexion(LocalDateTime.parse(entree));
        if (sortie != null) p.setDeconnexion(LocalDateTime.parse(sortie));
        return p;
    }
    private CalculPointage.Resultat calcul(Planning planning, Pointage... sessions) {
        return CalculPointage.calculer(date, planning, List.of(sessions), date.plusDays(1).atTime(12, 0));
    }

    @Test void calculeRetardPauseEtDepassementSansArrondiIntermediaire() {
        var r = calcul(planning("08:00", "17:00", "12:00", "13:00"), session("2026-11-09T08:15:30", "2026-11-09T18:00:30"));
        assertEquals(8 * 3600, r.prevu()); assertEquals(8 * 3600 + 45 * 60, r.travail());
        assertEquals(15 * 60 + 30, r.retard()); assertEquals(45 * 60, r.supplementaires());
        assertEquals(15 * 60 + 30, r.absence()); assertTrue(r.anomalies().contains(RETARD));
    }

    @Test void cumuleLesReconnexionsSansDeduireDeuxFoisLaPause() {
        var r = calcul(planning("08:00", "17:00", "12:00", "13:00"),
                session("2026-11-09T08:00", "2026-11-09T12:00"), session("2026-11-09T13:00", "2026-11-09T17:00"));
        assertEquals(8 * 3600, r.travail()); assertEquals(0, r.absence()); assertEquals(0, r.supplementaires());
    }

    @Test void deduitUniquementLaPartieDePauseRecouverte() {
        var r = calcul(planning("08:00", "17:00", "12:00", "13:00"), session("2026-11-09T12:30", "2026-11-09T17:00"));
        assertEquals(4 * 3600, r.travail()); assertEquals(4 * 3600, r.absence());
    }

    @Test void reunifieLesSessionsSimultanees() {
        var r = calcul(planning("08:00", "17:00", "12:00", "13:00"),
                session("2026-11-09T08:00", "2026-11-09T14:00"), session("2026-11-09T10:00", "2026-11-09T17:00"));
        assertEquals(8 * 3600, r.travail()); assertTrue(r.anomalies().contains(SESSIONS_SIMULTANEES));
    }

    @Test void traiteHorairesEtPauseTraversantMinuit() {
        var r = calcul(planning("22:00", "06:00", "23:45", "00:15"), session("2026-11-09T22:00", "2026-11-10T06:00"));
        assertEquals(7 * 3600 + 30 * 60, r.prevu()); assertEquals(r.prevu(), r.travail()); assertEquals(0, r.absence());
    }

    @Test void montreUneDureeProvisoireEtNeComptePasUneSortieInventee() {
        Planning planning = planning("08:00", "17:00", "12:00", "13:00");
        Pointage ouverte = session("2026-11-09T08:00", null);
        var live = CalculPointage.calculer(date, planning, List.of(ouverte), date.atTime(12, 30));
        assertTrue(live.enCours()); assertEquals(4 * 3600, live.travail());
        ouverte.setSessionExpiree(true);
        var expiree = calcul(planning, ouverte, session("2026-11-09T14:00", "2026-11-09T17:00"));
        assertEquals(3 * 3600, expiree.travail()); assertTrue(expiree.anomalies().contains(SORTIE_MANQUANTE));
        assertNull(ouverte.getDeconnexion()); assertFalse(expiree.enCours());
    }

    @Test void signaleAbsenceReposEtPlanningManquant() {
        var absence = calcul(planning("08:00", "17:00", null, null));
        assertEquals(9 * 3600, absence.absence()); assertTrue(absence.anomalies().contains(ABSENCE));
        Planning repos = new Planning(); repos.setJourTravaille(false);
        var travailRepos = calcul(repos, session("2026-11-09T08:00", "2026-11-09T10:00"));
        assertEquals(2 * 3600, travailRepos.supplementaires()); assertTrue(travailRepos.anomalies().contains(HORS_PLANNING));
        assertTrue(calcul(null, session("2026-11-09T08:00", "2026-11-09T10:00")).anomalies().contains(PLANNING_MANQUANT));
    }

    @Test void signaleDepartAnticipeEtHorairesIncoherents() {
        Planning planning = planning("08:00", "17:00", null, null);
        assertTrue(calcul(planning, session("2026-11-09T08:00", "2026-11-09T16:00")).anomalies().contains(DEPART_ANTICIPE));
        var invalide = calcul(planning, session("2026-11-09T10:00", "2026-11-09T08:00"));
        assertEquals(0, invalide.travail()); assertTrue(invalide.anomalies().contains(HORAIRES_INCOHERENTS));
    }
}
