-- PostgreSQL : appliquer sur une base existante avant le redemarrage.
BEGIN;
ALTER TABLE contrat DROP CONSTRAINT IF EXISTS contrat_statut_check;
UPDATE contrat SET statut = 'FINALISE' WHERE statut IN ('BROUILLON', 'A_SIGNER', 'SIGNE');
ALTER TABLE contrat ALTER COLUMN statut SET DEFAULT 'FINALISE';
ALTER TABLE contrat ADD CONSTRAINT contrat_statut_check CHECK (statut = 'FINALISE');
ALTER TABLE contrat DROP COLUMN IF EXISTS signe_le;
ALTER TABLE contrat DROP COLUMN IF EXISTS id_signataire;
ALTER TABLE contrat DROP COLUMN IF EXISTS revision;
UPDATE candidature c
SET id_etape = (SELECT id FROM etape_recrutement WHERE code = 'EMBAUCHE'),
    id_statut = (SELECT id FROM statut_candidature WHERE code = 'EMBAUCHE')
WHERE c.id_statut = (SELECT id FROM statut_candidature WHERE code = 'EN_COURS')
  AND c.id_etape IN (SELECT id FROM etape_recrutement WHERE code IN ('CONDITIONS', 'EMBAUCHE'))
  AND EXISTS (SELECT 1 FROM contrat co WHERE co.id_candidature = c.id AND co.statut = 'FINALISE');
COMMIT;
