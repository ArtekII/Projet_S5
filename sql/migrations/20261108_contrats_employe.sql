-- PostgreSQL : appliquer sur une base existante avant de redemarrer l'application.
-- Rend les contrats rattachables a un employe pour gerer les contrats successifs
-- (CE -> CDI, renouvellement CE, contrat programme a une date future...).
BEGIN;

-- Un contrat successif n'a pas de candidature : id_candidature devient nullable.
ALTER TABLE contrat ALTER COLUMN id_candidature DROP NOT NULL;

-- Supprime la contrainte d'unicite heritee de l'ancien modele (un contrat par candidature).
DO $$
DECLARE contrainte record;
BEGIN
    FOR contrainte IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'contrat'::regclass
          AND contype = 'u'
          AND pg_get_constraintdef(oid) LIKE '%(id_candidature)%'
    LOOP
        EXECUTE format('ALTER TABLE contrat DROP CONSTRAINT %I', contrainte.conname);
    END LOOP;
END $$;

ALTER TABLE contrat ADD COLUMN IF NOT EXISTS id_employe INT;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS duree_mois INT;

ALTER TABLE contrat DROP CONSTRAINT IF EXISTS contrat_duree_mois_check;
ALTER TABLE contrat ADD CONSTRAINT contrat_duree_mois_check
    CHECK (duree_mois IS NULL OR duree_mois > 0);

ALTER TABLE contrat DROP CONSTRAINT IF EXISTS contrat_statut_check;
ALTER TABLE contrat ADD CONSTRAINT contrat_statut_check
    CHECK (statut IN ('FINALISE', 'PROGRAMME'));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'contrat'::regclass AND conname = 'fk_contrat_employe'
    ) THEN
        ALTER TABLE contrat
            ADD CONSTRAINT fk_contrat_employe
            FOREIGN KEY (id_employe) REFERENCES employe(id_employe);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_contrat_employe ON contrat(id_employe);

-- Rattache les contrats deja crees a leur employe.
UPDATE contrat c
SET id_employe = e.id_employe
FROM employe e
WHERE e.id_contrat_actuel = c.id
  AND c.id_employe IS NULL;

COMMIT;
