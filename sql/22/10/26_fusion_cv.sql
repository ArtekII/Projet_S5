-- ===========================================
-- Fichier: 26_fusion_cv.sql
-- Date: 22/10/2026
-- Fusion de cv et cv_candidat (etat cible).
-- Idempotent : peut etre relance sans erreur.
-- ===========================================

-- 1. cv recupere les champs propres au depot de candidature.
ALTER TABLE cv ADD COLUMN IF NOT EXISTS annee_experience INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cv ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- 2. Copie des donnees de cv_candidat vers cv (si cv_candidat existe encore).
UPDATE cv c
SET annee_experience = COALESCE(cvc.annee_experience, c.annee_experience, 0),
    email = COALESCE(cvc.email, c.email)
FROM candidature ca
JOIN cv_candidat cvc
  ON cvc.id_candidat = ca.id_candidat
 AND cvc.id_offre = ca.id_offre
WHERE ca.id_cv = c.id;

UPDATE cv c
SET annee_experience = COALESCE(cvc.annee_experience, c.annee_experience, 0),
    email = COALESCE(cvc.email, c.email)
FROM (
    SELECT DISTINCT ON (id_candidat)
           id_candidat, annee_experience, email
    FROM cv_candidat
    ORDER BY id_candidat, created_at DESC, id DESC
) cvc
WHERE cvc.id_candidat = c.id_candidat
  AND c.email IS NULL;

-- 3. La table cv_candidat n'est plus utilisee par l'application.
--    A executer une fois les donnees verifiees dans cv :
-- DROP TABLE IF EXISTS cv_candidat;
