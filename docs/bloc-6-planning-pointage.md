# Bloc 6 : planning et pointage

Sur une base existante, appliquer `sql/migrations/20261108_planning_pointage.sql`
après les migrations des blocs 4 et 5, puis redémarrer l'application.

## Parcours

1. Les RH ouvrent `/rh/plannings`, choisissent un poste, un jour de semaine
   (lundi = 1, dimanche = 7), une date d'effet, les horaires et la pause facultative.
   Décocher « Jour travaillé » définit un jour de repos. Répéter pour les jours concernés.
2. La connexion d'un utilisateur ayant un dossier employé et un contrat applicable
   crée un pointage. « Se déconnecter » enregistre la sortie de cette session.
   Les candidats non encore embauchés ne pointent pas.
3. L'employé consulte `/espaceEmploye?onglet=pointages` : semaine de planning,
   sept derniers jours de pointage, temps travaillé, retards, heures supplémentaires
   et notifications. La page se rafraîchit toutes les 30 secondes tant qu'une session
   est en cours et que l'onglet est visible.
4. Les RH contrôlent `/rh/pointages`, filtrent une période de 31 jours maximum,
   vérifient les anomalies et régularisent les horaires si nécessaire. Chaque
   régularisation exige un motif et conserve les horaires d'origine, les valeurs
   précédemment retenues, les nouvelles valeurs, l'auteur et la date.
5. Le bouton « Valider cette journée et transmettre à Finance » crée un relevé
   définitif pour une journée passée et terminée. Une journée ouverte, sans planning
   ou comportant des horaires incohérents ne peut pas être transmise.
6. Finance retrouve les relevés dans `/finance/pointages` et les exporte en CSV.
   Les valeurs exportées sont des **secondes**, sans arrondi intermédiaire ni
   calcul de salaire, de majoration ou de retenue monétaire.

## Règles de calcul

- Toutes les dates et heures utilisent `Indian/Antananarivo`.
- Le poste est celui du contrat applicable à la date du pointage, ou de l'offre
  du dossier CV d'embauche si le contrat ne le précise pas directement. Les
  affectations libres du dossier administratif ne remplacent pas ce rattachement.
- Les horaires hebdomadaires sont versionnés par poste, jour et date d'effet.
  Les pointages déjà rattachés à une version conservent cette version. Une
  définition tardive peut compléter une journée qui n'avait pas de planning.
- Une fin antérieure au début désigne un horaire traversant minuit. Une connexion
  après minuit, avant la fin d'un tel horaire, est rattachée à la veille.
- Les périodes de connexion sont réunies avant le calcul : plusieurs sessions
  simultanées ne doublent pas le temps travaillé. Les périodes déconnectées
  ne sont pas comptées.
- La pause n'est déduite que pour sa partie recouvrant une période connectée.
  Une déconnexion pendant la pause ne conduit donc pas à une double déduction.
- Le retard est l'écart positif entre la première entrée et le début prévu.
- Les heures supplémentaires sont `max(temps travaillé net - durée prévue nette, 0)`.
  Une connexion un jour de repos est signalée et sa durée est supplémentaire.
- Le temps prévu non travaillé est le temps manquant **dans** l'horaire prévu,
  hors pause, une fois la plage terminée. Il peut coexister avec des heures
  supplémentaires lorsque des heures sont effectuées en dehors du planning.
- Sans sortie, le temps affiché est provisoire. Après expiration de la session,
  après plus de 24 heures ou plus de quatre heures après la fin prévue, la
  période ouverte est exclue des durées et signalée « Déconnexion manquante ».
  Aucune heure de sortie n'est inventée. Les périodes fermées restent comptées.
- Fermer un navigateur ne fournit pas d'heure exacte de départ au serveur.
  L'expiration de session et la détection périodique signalent ce cas pour les RH.
  Le pointage mesure le temps connecté à l'application, pas une présence physique.

## Alertes et transmission

Les notifications internes sont visibles dans l'espace employé et le suivi RH :
planning manquant, absence, retard, départ anticipé, heures supplémentaires,
connexion un jour de repos, sessions simultanées et sortie manquante ou incohérente.
Une seule alerte est conservée par employé, date et type. Les alertes devenues
inapplicables après une régularisation sont marquées résolues.

La détection automatique couvre les sept derniers jours et toutes les anciennes
sessions encore ouvertes. Consulter un historique RH recalcule aussi ses anomalies.
Elle est configurable par `app.pointage.detection-active` (défaut `true`),
`app.pointage.detection-intervalle-ms` et `app.pointage.detection-delai-initial-ms`
(défaut 60 000 ms chacun).

Une transmission conserve les durées, la version du planning, le validateur RH
et la date de transmission. La même journée n'est pas transmise deux fois et
ne peut plus être régularisée après transmission. Les avertissements non bloquants
(retard, absence, heures supplémentaires, etc.) restent soumis à la validation RH.
Les pauses et les relevés de paie ne peuvent pas être modifiés par un employé.
Les créations de planning, corrections et transmissions contrôlent un jeton de session.
