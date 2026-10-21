Mise à jour du parcours contrat et des délais
============================================

Avant de relancer l’application sur une base existante, appliquer
`20261021_contrats_delais.sql` avec le client PostgreSQL habituel. Ce script est
transactionnel et peut être réexécuté. Pour une base neuve, utiliser le schéma SQL
mis à jour.

Les contrats historiques sans signature dont la candidature était encore
EN_COURS reviennent à l’étape CONDITIONS, puis suivent la préparation RH et la
signature. Les candidatures déjà clôturées ne sont pas rouvertes.

Le délai est configurable avec `app.conditions.delai-jours` (7 par défaut).
La publication automatique vérifie les candidatures toutes les 60 secondes et
crée une copie des conditions du poste avec son échéance, même sans visite du
candidat. L’ouverture des écrans initialise aussi les propositions disponibles.
Le délai commence à cette publication, en heure de Madagascar.

Une contre-proposition suspend l’attente de réponse du candidat. Toute décision
RH accorde un nouveau délai complet. À échéance, les actions candidat sont
bloquées côté serveur, mais la candidature n’est pas automatiquement éliminée.
Le RH peut prolonger de 7 jours à partir de l’échéance actuelle, ou de maintenant
si elle est dépassée. Les versions et réponses antérieures sont conservées.

Le contrat passe de BROUILLON à A_SIGNER après préparation des dates, puis à
SIGNE après confirmation explicite du candidat. Son identité et la date de
confirmation sont enregistrées. La confirmation porte sur la version affichée :
une modification des dates impose de recharger le contrat. Cette fonctionnalité
enregistre un accord dans l’application ; elle n’intègre pas un prestataire de
signature électronique.
