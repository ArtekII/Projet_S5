package com.project.dto;

public enum AnomaliePointage {
    PLANNING_MANQUANT("Aucun planning applicable : les RH doivent définir les horaires du poste.", true),
    SORTIE_MANQUANTE("Déconnexion manquante : une régularisation RH est nécessaire.", true),
    HORAIRES_INCOHERENTS("Horaires incohérents ou session de plus de 24 heures : vérification RH nécessaire.", true),
    ABSENCE("Aucune connexion pendant une journée de travail prévue.", false),
    RETARD("Arrivée après l'heure prévue par le planning.", false),
    DEPART_ANTICIPE("Dernière déconnexion avant l'heure de fin prévue.", false),
    HEURES_SUP("Le temps travaillé dépasse la durée prévue par le planning.", false),
    HORS_PLANNING("Connexion pendant un jour de repos.", false),
    SESSIONS_SIMULTANEES("Plusieurs sessions se recouvrent ; leur durée est comptée une seule fois.", false);

    private final String message;
    private final boolean bloquante;
    AnomaliePointage(String message, boolean bloquante) { this.message = message; this.bloquante = bloquante; }
    public String getMessage() { return message; }
    public boolean isBloquante() { return bloquante; }
}
