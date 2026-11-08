# Bloc 7 : gestion des congés

Sur une base existante, appliquer `sql/migrations/20261108_conges.sql`
après les migrations des blocs 4, 5 et 6, puis redémarrer l'application.

## Parcours

1. Un employé ouvre `/espaceEmploye?onglet=conges`. Il consulte ses soldes,
   choisit un type de congé et une période, puis envoie sa demande.
2. Une demande en attente peut être annulée par son auteur tant qu'elle n'a pas
   été traitée. Elle réserve le solde mais ne le consomme pas.
3. Les RH ouvrent `/rh/conges`, contrôlent le solde et valident ou refusent chaque
   demande. Le motif de décision est facultatif.
4. La validation décompte les jours ouvrés du solde. Le refus et l'annulation
   ne modifient jamais le solde consommé.
5. Chaque création, validation, annulation et refus est conservée dans
   `historique_conge`, avec l'utilisateur auteur de l'action.

## Règles de calcul

- Les dates utilisent `Indian/Antananarivo`.
- Une période de congés dure douze mois et commence à la date d'embauche
  (puis à chaque anniversaire). Seule la période en cours est alimentée
  automatiquement.
- Chaque mois de la période ajoute `type_conge.acquisition_mensuelle` au solde.
  Le mois en cours est compté dès son premier jour et l'acquisition est plafonnée
  à douze mensualités par période.
- Seuls les types avec `decompte_solde = true` et une acquisition mensuelle non
  nulle créent et consomment automatiquement un solde. Les types sans décompte
  (maladie, sans solde) restent traçables sans bloquer la demande.
- Les jours décomptés sont les jours ouvrés du lundi au vendredi, bornes incluses.
  Les jours fériés ne sont pas encore déduits.
- Une demande doit commencer aujourd'hui ou plus tard, contenir au moins un jour
  ouvré et ne pas chevaucher une autre demande en attente ou validée.
- Une demande à cheval sur deux périodes annuelles est refusée : elle doit se
  terminer dans la période de son jour de début.
- Le solde affiché comme disponible tient compte des demandes en attente. La
  validation vérifie à nouveau ce reste pour éviter deux validations concurrentes
  qui dépasseraient le solde.

## Types de congés fournis

La migration et l'initialiseur créent les types de base s'ils n'existent pas :

| Code | Libellé | Décompté | Acquisition mensuelle |
| --- | --- | --- | --- |
| `CP` | Congés payés | oui | 2,50 jours |
| `RTT` | Réduction du temps de travail | oui | 1,00 jour |
| `MAL` | Arrêt maladie | non | aucune |
| `SANS_SOLDE` | Congé sans solde | non | aucune |

Un administrateur peut ajouter d'autres types dans `type_conge` : renseigner
`acquisition_mensuelle` pour activer l'acquisition et le décompte automatiques.

## Commandes utiles

```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d projetTontonTovo \
  -v ON_ERROR_STOP=1 -f sql/migrations/20261108_conges.sql
```

Le script est réexécutable. Il répare aussi une ancienne table
`historique_conge` dont `id_auteur` référençait `employe`, afin que les décisions
RH puissent être attribuées à un `utilisateur`.
