
package com.project.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.entity.CandidatsSelec;

/**
 * Vue d'une etape de candidature (ligne de la table {@code candidats_selec})
 * prete a l'affichage : nom de l'etape, note, rang et date de selection.
 */
public class EtapeVue {

    private final String nom;
    private final BigDecimal note;
    private final Integer rang;
    private final LocalDateTime dateSelection;

    private EtapeVue(String nom, BigDecimal note, Integer rang, LocalDateTime dateSelection) {
        this.nom = nom;
        this.note = note;
        this.rang = rang;
        this.dateSelection = dateSelection;
    }

    /** Construit la vue a partir d'une ligne {@code candidats_selec}. */
    public static EtapeVue from(CandidatsSelec selection) {
        String nom = selection.getEtape() != null ? selection.getEtape().getNom() : "";
        return new EtapeVue(pretty(nom), selection.getNote(), selection.getRang(),
                selection.getDateSelection());
    }

    /** "selection" -> "Selection" (mise en forme lisible du nom stocke en base). */
    private static String pretty(String nom) {
        if (nom == null || nom.isBlank()) {
            return "Etape";
        }
        return Character.toUpperCase(nom.charAt(0)) + nom.substring(1);
    }

    public String getNom() {
        return nom;
    }

    public BigDecimal getNote() {
        return note;
    }

    public Integer getRang() {
        return rang;
    }

    public LocalDateTime getDateSelection() {
        return dateSelection;
    }
}
