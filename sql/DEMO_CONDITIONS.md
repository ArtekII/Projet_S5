# Candidats selectionnes apres l'entretien

Trois candidats sont retenus pour trois places de Developpeur Junior.

| Compte | Note entretien |
| --- | --- |
| alice@demo.com | 54/60 |
| bob@demo.com | 51/60 |
| clara@demo.com | 48/60 |

Mot de passe commun : `Test1234!`.
Se connecter puis ouvrir « Mes conditions » (/candidat/conditions).
Le passage de ENTRETIEN a CONDITIONS se fait automatiquement.
Les comptes RH et manager habituels sont utilises, sans comptes supplementaires.

Pour charger les donnees sur une base initialisee :

```powershell
psql -h localhost -U postgres -d projetTontonTovo -v ON_ERROR_STOP=1 -f sql/demo_conditions_entretien.sql
```

Le script peut etre rejoue sans dupliquer les candidats ni effacer leurs reponses.
