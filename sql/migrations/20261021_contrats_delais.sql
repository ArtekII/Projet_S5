BEGIN;
ALTER TABLE condition_travail ADD COLUMN IF NOT EXISTS repondre_avant TIMESTAMP;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON';
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS signe_le TIMESTAMP;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS id_signataire INT REFERENCES utilisateur(id);
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS id_rh_preparation INT REFERENCES utilisateur(id);

UPDATE candidature c
SET id_etape = (SELECT id FROM etape_recrutement WHERE code = 'CONDITIONS')
WHERE c.id_etape = (SELECT id FROM etape_recrutement WHERE code = 'EMBAUCHE')
  AND c.id_statut = (SELECT id FROM statut_candidature WHERE code = 'EN_COURS')
  AND EXISTS (SELECT 1 FROM contrat co WHERE co.id_candidature = c.id AND co.statut = 'BROUILLON' AND co.signe_le IS NULL);
COMMIT;
