Appliquer `20261021_attribution_directe.sql` puis `20261106_employe.sql` et
enfin `20261108_contrats_employe.sql` sur la base existante avant de
redémarrer l’application. Ces scripts peuvent être réexécutés.

Tout contrat déjà créé est considéré comme attribué. Les candidatures liées
encore en cours passent à l’embauche ; les dossiers refusés restent clos.
Les dates historiques sont conservées mais ne nécessitent plus aucune action.

Le RH accorde l’accréditation CDI depuis les conditions de la candidature,
avant son acceptation. Le candidat obtient un CDI si cdi_autorise est vrai,
sinon un CE. L’acceptation crée le contrat et confirme immédiatement l’embauche.

Depuis la page `/rh/contrats`, le RH peut ensuite faire évoluer le contrat
d’un employé réel :

- une date de commencement aujourd’hui ou passée applique immédiatement le
  nouveau contrat (par exemple CE → CDI) ;
- une date future programme un contrat successif, activé automatiquement à
  cette date par l’application ;
- chaque contrat précédent reste consultable dans l’historique de l’employé.

Pour les blocs 5 et 6, appliquer ensuite `20261108_dossier_administratif.sql`
puis `20261108_planning_pointage.sql`, avant de redémarrer l'application.
Le second script crée les plannings, pointages, alertes, corrections et variables
de paie. Il peut être réexécuté sans effacer les données.

Le fonctionnement et les règles de calcul du bloc 6 sont décrits dans
[`docs/bloc-6-planning-pointage.md`](../../docs/bloc-6-planning-pointage.md).
