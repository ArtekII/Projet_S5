# Guide RH : candidature et sélection

Ce document décrit l'ordre conseillé pour implémenter le parcours :

```text
Candidature CV → Sélection CV → Questionnaire → Top 5 → Entretien → Classement final
```

> Le formulaire CV, la première sélection, le questionnaire, le Top 5, les
> entretiens et le classement final sont maintenant branchés dans l'application.

## 1. Préparer la base de données

Avant de coder les pages, ajouter les tables dans un script SQL de migration.
Chaque sélection doit être liée à une **offre**, et pas seulement à un poste :
une même offre peut avoir plusieurs postes ou plusieurs sessions de recrutement.

Tables principales à prévoir :

```sql
cv_candidat (
    id, id_candidat, id_offre, nom, prenom, date_naissance,
    annee_experience, email
)

candidats_selec (
    id, id_offre, id_cv_candidat, id_etape, note, rang, date_selection
)

questions_teste (
    id, id_offre, question, points_max
)

entretien_qualification (
    id, id_offre, nom
)

entretien_candidat (
    id, id_eq, id_candidat_selec, points, commentaire
)
```

Ajouter des contraintes d'unicité pour empêcher une double candidature ou une
double sélection :

```sql
UNIQUE (id_candidat, id_offre)
UNIQUE (id_offre, id_cv_candidat, id_etape)
```

Les clés étrangères doivent utiliser `ON DELETE CASCADE` lorsque la ligne
dépend entièrement d'une candidature.

## 2. Créer une candidature avec le formulaire CV

### Interface candidat

Dans la page de détail d'une offre publiée, remplacer le bouton désactivé
« Postuler » par un formulaire contenant :

- nom ;
- prénom ;
- date de naissance ;
- nombre d'années d'expérience ;
- offre sélectionnée, envoyée dans un champ caché.

Le candidat connecté doit être récupéré depuis la session. Il ne faut pas
prendre `id_candidat` depuis un champ HTML modifiable.

### Backend

Créer :

- une entité `CvCandidat` ;
- un repository `CvCandidatRepository` ;
- une méthode `POST /candidat/offre/{offreId}/candidature` ;
- une validation des champs ;
- une vérification que l'offre possède le statut `PUBLIEE`.

À la validation du formulaire :

1. récupérer le candidat connecté ;
2. récupérer l'offre publiée ;
3. vérifier qu'il n'a pas déjà candidaté ;
4. enregistrer le CV dans `cv_candidat` ;
5. afficher un message de confirmation.

## 3. Faire la première sélection des CV

### Page RH

Ajouter une page accessible uniquement au RH, par exemple :

```text
/rh/offre/{offreId}/candidatures
```

Afficher les CV d'une offre publiée ou d'une offre en cours de recrutement :

- nom ;
- prénom ;
- date de naissance ;
- années d'expérience ;
- date de candidature.

Trier la liste par années d'expérience décroissantes :

```java
findByOffreIdOrderByAnneeExperienceDesc(Integer offreId)
```

Chaque ligne contient une case à cocher. Le RH sélectionne les candidats puis
clique sur **Valider la première sélection**.

### Enregistrement

Pour chaque CV coché, insérer une ligne dans `candidats_selec` avec
`id_offre`, `id_cv_candidat` et l'étape `selection`.

Après validation :

1. empêcher une nouvelle sélection identique ;
2. afficher les candidats sélectionnés ;
3. proposer le passage à la création du questionnaire.

## 4. Créer et afficher le questionnaire

### Création par le RH

Créer une page :

```text
/rh/offre/{offreId}/questionnaire
```

Le RH peut ajouter une ou plusieurs questions liées à l'offre :

- texte de la question ;
- nombre de points maximum.

Enregistrer les questions dans `questions_teste`.

### Affichage candidat

Le questionnaire doit être visible uniquement pour les candidats présents dans
`candidats_selec` à l'étape `selection` pour cette offre.

Créer une page, par exemple :

```text
/candidat/offre/{offreId}/questionnaire
```

Le candidat répond ou, selon le fonctionnement demandé, le RH saisit directement
la note dans un formulaire de correction. Dans les deux cas, la note doit être
liée à `id_candidat_selec1`.

## 5. Calculer la deuxième sélection : Top 5

Créer une action RH :

```text
POST /rh/offre/{offreId}/selection-questionnaire
```

Étapes :

1. récupérer les candidats sélectionnés à l'étape 1 ;
2. récupérer leur nombre de points ;
3. trier par `points DESC` ;
4. conserver les cinq meilleurs ;
5. enregistrer les cinq lignes dans `candidats_selec` à l'étape `Test` ;
6. afficher la liste retenue.

En cas d'égalité autour de la cinquième place, choisir une règle et l'appliquer
toujours. Règle simple : trier d'abord par points décroissants, puis par date de
candidature croissante.

## 6. Organiser les entretiens

Le RH crée un type ou une grille d'entretien dans
`entretien_qualification`, par exemple :

- motivation ;
- compétences techniques ;
- communication ;
- disponibilité.

Pour chaque candidat du Top 5, afficher un formulaire :

- entretien choisi ;
- points obtenus ;
- commentaire éventuel.

Enregistrer le résultat dans `entretien_candidat`.

Vérifier qu'un même candidat ne possède pas deux résultats pour le même
entretien, par exemple avec :

```sql
UNIQUE (id_EQ, id_candidat_selec2)
```

## 7. Calculer la sélection finale

Créer une action RH :

```text
POST /rh/offre/{offreId}/selection-finale
```

Étapes :

1. récupérer les candidats du Top 5 ;
2. calculer la note finale à partir des points d'entretien ;
3. trier par note finale décroissante ;
4. attribuer les rangs `1`, `2`, `3`, etc. ;
5. enregistrer les résultats dans `candidats_selec` à l'étape `Entretien`.

Afficher un tableau avec :

- nom et prénom ;
- note finale ;
- rang ;
- statut : retenu ou non retenu.

Le nombre de personnes retenues peut être limité à `nb_places` de l'offre.

## 8. Sécuriser les accès

Les routes doivent respecter les rôles :

- `CANDIDAT` : remplir son CV, candidater et voir son questionnaire ;
- `RH` : voir les candidatures, noter les questionnaires, organiser les entretiens
  et valider les sélections ;
- `MANAGER`, `FINANCE` et `DIRECTION` : ne doivent pas modifier les sélections RH.

À chaque requête, vérifier aussi que l'objet appartient bien à l'offre demandée.
Un candidat ne doit jamais pouvoir modifier l'identifiant d'un autre candidat
en changeant simplement l'URL.

## 9. Utilisation dans l'application

1. La Direction publie une offre.
2. Le candidat ouvre le détail de l'offre et envoie son CV.
3. Le RH ouvre son dashboard puis clique sur **Ouvrir** dans la colonne
   **Recrutement** de l'offre.
4. Le RH coche les CV et valide la première sélection.
5. Le RH ajoute les questions puis saisit les notes des candidats sélectionnés.
6. L'application conserve les cinq meilleures notes dans `candidats_selec` à
   l'étape `Test`.
7. Le RH ajoute une grille d'entretien et saisit les notes.
8. L'application calcule et affiche le classement final dans `candidats_selec` à
   l'étape `Entretien`.
9. Un candidat sélectionné peut consulter son questionnaire depuis le détail de
   l'offre.

## 10. Ordre conseillé des fichiers Java

Implémenter dans cet ordre :

1. `CvCandidat` + repository ;
2. formulaire et contrôleur candidat ;
3. `CandidatSelec` + `Etape` + sélection RH ;
4. `QuestionsTeste` + questionnaire ;
5. `CandidatSelec` à l'étape `Test` + calcul du Top 5 ;
6. `EntretienQualification` et `EntretienCandidat` ;
7. `CandidatSelec` à l'étape `Entretien` + classement final ;
8. contrôles de rôle et tests.

## 11. Vérifications à faire

Tester le parcours avec plusieurs comptes :

1. Direction publie une offre ;
2. un candidat remplit son CV ;
3. le RH voit le CV dans la bonne offre ;
4. le RH valide la première sélection ;
5. seuls les candidats sélectionnés voient le questionnaire ;
6. le RH saisit les points ;
7. les cinq meilleurs passent à l'étape `Test` dans `candidats_selec` ;
8. le RH saisit les points d'entretien ;
9. le classement final à l'étape `Entretien` est correct ;
10. une deuxième candidature identique est refusée.
