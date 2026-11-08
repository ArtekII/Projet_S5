package com.project.service;

import static org.junit.jupiter.api.Assertions.*;

import com.project.entity.TypeConge;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CongeCalculTest {
    @Test
    void calculeLesJoursOuvresSansWeekend() {
        LocalDate lundi = LocalDate.of(2026, 11, 9);
        assertEquals(new BigDecimal("5.00"), CongeService.joursOuvres(lundi, lundi.plusDays(6)));
        assertEquals(new BigDecimal("1.00"), CongeService.joursOuvres(lundi, lundi));
        assertEquals(new BigDecimal("0.00"), CongeService.joursOuvres(lundi.plusDays(5), lundi.plusDays(6)));
        assertEquals(new BigDecimal("0.00"), CongeService.joursOuvres(lundi, lundi.minusDays(1)));
    }

    @Test
    void decoupeLaPeriodeAnnuelleSurLaDateDEmbauche() {
        LocalDate embauche = LocalDate.of(2025, 11, 8);
        CongeService.Periode premiere = CongeService.periodePour(embauche, LocalDate.of(2026, 11, 7));
        assertEquals(LocalDate.of(2025, 11, 8), premiere.debut());
        assertEquals(LocalDate.of(2026, 11, 7), premiere.fin());
        CongeService.Periode seconde = CongeService.periodePour(embauche, LocalDate.of(2026, 11, 8));
        assertEquals(LocalDate.of(2026, 11, 8), seconde.debut());
        assertEquals(LocalDate.of(2027, 11, 7), seconde.fin());
        assertNull(CongeService.periodePour(embauche, embauche.minusDays(1)));
    }

    @Test
    void acquiertUneMensualiteParMoisEtPlafonneADouze() {
        TypeConge cp = new TypeConge();
        cp.setDecompteSolde(true);
        cp.setAcquisitionMensuelle(new BigDecimal("2.50"));
        CongeService.Periode periode = new CongeService.Periode(
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31));
        assertEquals(new BigDecimal("2.50"), CongeService.joursAcquis(cp, periode, LocalDate.of(2026, 9, 1)));
        assertEquals(new BigDecimal("7.50"), CongeService.joursAcquis(cp, periode, LocalDate.of(2026, 11, 9)));
        assertEquals(new BigDecimal("30.00"), CongeService.joursAcquis(cp, periode, LocalDate.of(2028, 1, 1)));
        assertEquals(new BigDecimal("0.00"), CongeService.joursAcquis(cp, periode, LocalDate.of(2026, 8, 31)));
    }

    @Test
    void ignoreLesTypesSansDecompteOuSansAcquisition() {
        TypeConge maladie = new TypeConge();
        maladie.setDecompteSolde(false);
        maladie.setAcquisitionMensuelle(new BigDecimal("2.50"));
        TypeConge manuel = new TypeConge();
        manuel.setDecompteSolde(true);
        manuel.setAcquisitionMensuelle(null);
        CongeService.Periode periode = new CongeService.Periode(
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31));
        assertEquals(new BigDecimal("0.00"), CongeService.joursAcquis(maladie, periode, LocalDate.of(2026, 11, 9)));
        assertEquals(new BigDecimal("0.00"), CongeService.joursAcquis(manuel, periode, LocalDate.of(2026, 11, 9)));
    }
}
