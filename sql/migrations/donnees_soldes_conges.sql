-- =====================================================================
-- Jeu de donnees : soldes de conges des employes de demonstration
-- (emp1@demo.com, emp2@demo.com, emp3@demo.com, emp4@demo.com, ...)
--
-- A executer APRES :
--   1. sql/migrations/20261108_conges.sql
--   2. sql/migrations/donnesemp.sql
--
-- Le script est reexecutable :
--   - il cree le solde de la periode annuelle en cours s'il manque ;
--   - il met a jour jours_acquis si le mois a avance ;
--   - il ne modifie jamais jours_pris (solde deja consomme).
--
-- Seuls les types actifs avec decompte_solde = TRUE et une acquisition
-- mensuelle non nulle sont alimentes (CP, RTT, etc.).
--
-- Exemple de resultat au 09/11/2026 :
--   emp1 : CP 7.50, RTT 3.00   (embauche le 01/09/2026)
--   emp2 : CP 5.00, RTT 2.00   (embauche le 01/10/2026)
--   emp3 : CP 12.50, RTT 5.00  (embauche le 01/07/2026)
--   emp4 : CP 2.50, RTT 1.00   (embauche le 02/11/2026)
-- =====================================================================
BEGIN;

WITH aujourdhui AS (
    SELECT (now() AT TIME ZONE 'Indian/Antananarivo')::date AS jour
),
base AS (
    SELECT e.id_employe,
           e.date_embauche,
           tc.id_type_conge,
           tc.code AS type_code,
           tc.acquisition_mensuelle,
           a.jour,
           (e.date_embauche
                + (EXTRACT(YEAR FROM age(a.jour, e.date_embauche))::int * INTERVAL '1 year'))::date
                AS periode_debut
    FROM employe e
    JOIN utilisateur u ON u.id = e.id_utilisateur
    JOIN type_conge tc
      ON tc.actif = TRUE
     AND tc.decompte_solde = TRUE
     AND tc.acquisition_mensuelle IS NOT NULL
    CROSS JOIN aujourdhui a
    WHERE u.email ~ '^emp[0-9]+@demo\.com$'
      AND e.date_embauche IS NOT NULL
      AND a.jour >= e.date_embauche
),
calcul AS (
    SELECT b.id_employe,
           b.id_type_conge,
           b.acquisition_mensuelle,
           b.periode_debut,
           (b.periode_debut + INTERVAL '1 year' - INTERVAL '1 day')::date AS periode_fin,
           LEAST(
               12,
               (EXTRACT(YEAR FROM age(b.jour, b.periode_debut)) * 12
                + EXTRACT(MONTH FROM age(b.jour, b.periode_debut))
                + 1)::int
           ) AS mois_acquis
    FROM base b
)
INSERT INTO solde_conge (
    id_employe,
    id_type_conge,
    periode_debut,
    periode_fin,
    jours_acquis,
    jours_pris,
    date_maj
)
SELECT c.id_employe,
       c.id_type_conge,
       c.periode_debut,
       c.periode_fin,
       ROUND(c.acquisition_mensuelle * c.mois_acquis, 2),
       0,
       now()
FROM calcul c
ON CONFLICT (id_employe, id_type_conge, periode_debut)
DO UPDATE SET
    periode_fin = EXCLUDED.periode_fin,
    jours_acquis = EXCLUDED.jours_acquis,
    date_maj = EXCLUDED.date_maj;

COMMIT;

-- ---------------------------------------------------------------------
-- Verification des soldes crees
-- ---------------------------------------------------------------------
SELECT e.id_employe,
       u.email,
       tc.code AS type_conge,
       s.periode_debut,
       s.periode_fin,
       s.jours_acquis,
       s.jours_pris,
       s.solde_disponible
FROM solde_conge s
JOIN employe e ON e.id_employe = s.id_employe
JOIN utilisateur u ON u.id = e.id_utilisateur
JOIN type_conge tc ON tc.id_type_conge = s.id_type_conge
WHERE u.email ~ '^emp[0-9]+@demo\.com$'
ORDER BY e.id_employe, tc.id_type_conge;

-- ---------------------------------------------------------------------
-- Nettoyage (optionnel) : supprimer les soldes des employes de demo
-- ---------------------------------------------------------------------
-- DELETE FROM solde_conge
-- WHERE id_employe IN (
--     SELECT e.id_employe
--     FROM employe e
--     JOIN utilisateur u ON u.id = e.id_utilisateur
--     WHERE u.email ~ '^emp[0-9]+@demo\.com$'
-- );
