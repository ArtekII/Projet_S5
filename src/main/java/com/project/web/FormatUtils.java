package com.project.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Fonctions utilitaires exposees aux vues JSP via la taglib
 * {@code /WEB-INF/tld/functions.tld} (prefixe {@code f} dans les JSP).
 *
 * Exemple d'utilisation : ${f:dateTime(offre.createdAt)}
 */
public final class FormatUtils {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatUtils() {
        // classe utilitaire : pas d'instanciation
    }

    /** Formate un LocalDateTime en "dd/MM/yyyy HH:mm" (ou "-" si null). */
    public static String dateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    /** Formate un LocalDateTime en "dd/MM/yyyy" (ou "-" si null). */
    public static String date(LocalDateTime value) {
        return value == null ? "-" : DATE.format(value);
    }
}
