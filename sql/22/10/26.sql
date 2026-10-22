-- ===========================================
-- Fichier: 22-10-26.sql
-- Date: 22/10/2026
-- Projet Recrutement - Espace candidat
-- Etapes de candidature (table candidats_selec) du candidat connecte :
-- le candidat@demo.com a postule a trois offres publiees et y a franchi
-- differentes etapes (selection, Entretien, Test).
-- Compatible PostgreSQL
-- ===========================================

-- 0. Libelles des etapes du processus (table etape)
UPDATE etape SET nom = 'Selection' WHERE lower(nom) = 'selection';

-- 1. CV du candidat de demo sur les offres publiees
WITH candidat AS (
    SELECT id FROM utilisateur WHERE email = 'candidat@demo.com'
), offres AS (
    -- une offre publiee par poste (la plus ancienne)
    SELECT DISTINCT ON (p.intitule) o.id, p.intitule
    FROM offre_emploi o
    JOIN statut_offre s ON s.id = o.id_statut
    JOIN poste p ON p.id = o.id_poste
    WHERE s.code = 'PUBLIEE'
      AND p.intitule IN ('Developpeur Junior', 'Developpeur Senior', 'Chef de projet')
    ORDER BY p.intitule, o.id
), a_creer AS (
    SELECT c.id AS id_candidat, o.id AS id_offre, o.intitule
    FROM candidat c CROSS JOIN offres o
    WHERE NOT EXISTS (
        SELECT 1 FROM cv_candidat cv WHERE cv.id_candidat = c.id AND cv.id_offre = o.id
    )
)
INSERT INTO cv_candidat (id_candidat, id_offre, nom, prenom, date_naissance,
                         annee_experience, email, created_at)
SELECT id_candidat, id_offre, 'Rasoanaivo', 'Luc', DATE '1997-04-12', 4,
       'candidat@demo.com',
       NOW() - (CASE intitule
                    WHEN 'Developpeur Junior' THEN 12
                    WHEN 'Developpeur Senior' THEN 6
                    ELSE 3
                END) * INTERVAL '1 day'
FROM a_creer;

-- 2. Etapes franchies par le candidat (table candidats_selec)
--    Les etapes sont resolues par leur nom pour ne pas dependre des identifiants.
INSERT INTO candidats_selec (id_offre, id_cv_candidat, id_etape, note, rang, date_selection)
SELECT cv.id_offre, cv.id, e.id, v.note, v.rang,
       NOW() - (v.jours * INTERVAL '1 day')
FROM cv_candidat cv
JOIN utilisateur u ON u.id = cv.id_candidat
JOIN offre_emploi o ON o.id = cv.id_offre
JOIN poste p ON p.id = o.id_poste
CROSS JOIN (VALUES
    ('Developpeur Junior', 'selection', 15.00::numeric, 1, 12),
    ('Developpeur Junior', 'Test', 17.50::numeric, 1, 8),
    ('Developpeur Junior', 'Entretien', 18.00::numeric, 1, 4),
    ('Developpeur Senior', 'selection', 12.00::numeric, 3, 6)
) AS v(intitule, nom_etape, note, rang, jours)
JOIN etape e ON e.nom = v.nom_etape
WHERE u.email = 'candidat@demo.com'
  AND p.intitule = v.intitule
  AND NOT EXISTS (
      SELECT 1 FROM candidats_selec cs WHERE cs.id_cv_candidat = cv.id
  );

-- 3. Cursus du candidat (table cv) : obligatoire pour la ligne "candidature"
INSERT INTO cv (id_candidat, nom, prenom, date_naissance, diplome, competences, hobby, created_at)
SELECT u.id, 'Rasoanaivo', 'Luc', DATE '1997-04-12', 'Master informatique',
       'Java, Spring Boot, SQL, JPA', 'Lecture', NOW() - INTERVAL '12 days'
FROM utilisateur u
WHERE u.email = 'candidat@demo.com'
  AND NOT EXISTS (SELECT 1 FROM cv WHERE cv.id_candidat = u.id);

-- 4. Candidatures du candidat (table candidature)
--    id_etape = etape_recrutement en cours, id_statut = statut de la candidature
INSERT INTO candidature (id_offre, id_candidat, id_cv, id_etape, id_statut,
                         score_questionnaire, cdi_autorise, created_at)
SELECT cv.id_offre, u.id, c.id,
       CASE p.intitule
           WHEN 'Developpeur Junior' THEN 2   -- questionnaire en cours
           WHEN 'Developpeur Senior' THEN 1   -- selection des CV
           ELSE 1
       END,
       CASE p.intitule
           WHEN 'Chef de projet' THEN 2       -- elimine
           ELSE 1                             -- en cours
       END,
       18.00, FALSE, cv.created_at
FROM cv_candidat cv
JOIN utilisateur u ON u.id = cv.id_candidat
JOIN cv c ON c.id_candidat = u.id
JOIN offre_emploi o ON o.id = cv.id_offre
JOIN poste p ON p.id = o.id_poste
JOIN statut_offre so ON so.id = o.id_statut
WHERE u.email = 'candidat@demo.com'
  AND so.code = 'PUBLIEE'
  AND p.intitule IN ('Developpeur Junior', 'Developpeur Senior', 'Chef de projet')
  AND NOT EXISTS (
      SELECT 1 FROM candidature ca WHERE ca.id_candidat = u.id AND ca.id_offre = cv.id_offre
  );

-- 5. Questionnaires des offres publiees (etape "questionnaire" du processus)
--    1 question QCM "experience Java" (3 choix) + 1 QCM "transactions" (3 choix)
--    + 1 reponse libre, pour chaque offre publiee.
INSERT INTO questionnaire (id_offre, titre, seuil_reussite, id_createur)
SELECT o.id, 'Questionnaire de recrutement - ' || p.intitule, 10.00,
       (SELECT u.id FROM utilisateur u JOIN role r ON r.id = u.id_role
        WHERE r.code = 'RH' ORDER BY u.id LIMIT 1)
FROM offre_emploi o
JOIN poste p ON p.id = o.id_poste
JOIN statut_offre s ON s.id = o.id_statut
WHERE s.code = 'PUBLIEE'
  AND NOT EXISTS (SELECT 1 FROM questionnaire q WHERE q.id_offre = o.id);

-- 5.1 Questions : id_type 1 = QCM, id_type 2 = reponse libre
INSERT INTO question (id_questionnaire, id_type, enonce, points, ordre)
SELECT q.id, v.id_type, v.enonce, v.points, v.ordre
FROM questionnaire q
CROSS JOIN (VALUES
    (1::integer, 'Quelle est votre experience avec Java ?', 2.00::numeric, 1),
    (1::integer, 'Comment gererez-vous une transaction metier ?', 3.00::numeric, 2),
    (2::integer, 'Decrivez une mission que vous avez menee de bout en bout.', 5.00::numeric, 3)
) AS v(id_type, enonce, points, ordre)
WHERE NOT EXISTS (
    SELECT 1 FROM question qu WHERE qu.id_questionnaire = q.id
);

-- 5.2 Choix de reponse des QCM (le premier choix correct est marque correct)
INSERT INTO choix_reponse (id_question, libelle, est_correct)
SELECT qu.id, v.libelle, v.est_correct
FROM question qu
JOIN questionnaire q ON q.id = qu.id_questionnaire
CROSS JOIN (VALUES
    (1, 'Moins d''un an', false),
    (1, 'Entre 1 et 3 ans', true),
    (1, 'Plus de 3 ans', true),
    (2, 'Sans transaction ni rollback', false),
    (2, 'Avec @Transactional et rollback', true),
    (2, 'En SQL manuel sans transaction', false)
) AS v(ordre, libelle, est_correct)
WHERE qu.ordre = v.ordre
  AND qu.id_type = 1
  AND NOT EXISTS (
      SELECT 1 FROM choix_reponse cr WHERE cr.id_question = qu.id
  );

-- 6. Etat de demonstration : la candidature "Developpeur Senior" du candidat de
--    demo attend la validation RH de l'etape 1 (selection des CV).
--    (les reponses au questionnaire de "Developpeur Junior" sont conservees)
DELETE FROM candidature_etape
WHERE id_candidature IN (
    SELECT c.id
    FROM candidature c
    JOIN utilisateur u ON u.id = c.id_candidat
    JOIN offre_emploi o ON o.id = c.id_offre
    JOIN poste p ON p.id = o.id_poste
    WHERE u.email = 'candidat@demo.com' AND p.intitule = 'Developpeur Senior'
);

UPDATE candidature SET id_etape = 1, id_statut = 1
WHERE id IN (
    SELECT c.id
    FROM candidature c
    JOIN utilisateur u ON u.id = c.id_candidat
    JOIN offre_emploi o ON o.id = c.id_offre
    JOIN poste p ON p.id = o.id_poste
    WHERE u.email = 'candidat@demo.com' AND p.intitule = 'Developpeur Senior'
);
