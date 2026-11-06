-- PostgreSQL : executer avant de redemarrer l'application.
BEGIN;
CREATE TABLE IF NOT EXISTS employe (
    id_employe INT PRIMARY KEY,
    id_utilisateur INT NOT NULL UNIQUE REFERENCES utilisateur(id),
    id_cv_candidat INT NOT NULL REFERENCES cv_candidat(id),
    id_contrat_actuel INT NOT NULL UNIQUE REFERENCES contrat(id),
    date_embauche DATE NOT NULL
);
-- Compatible avec la table deja creee par sql/6/11/26.sql.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'employe'
          AND column_name = 'id_employe' AND is_identity = 'YES'
    ) THEN
        CREATE SEQUENCE IF NOT EXISTS employe_id_seq OWNED BY employe.id_employe;
        ALTER TABLE employe ALTER COLUMN id_employe SET DEFAULT nextval('employe_id_seq');
        PERFORM setval('employe_id_seq', COALESCE(MAX(id_employe), 0) + 1, false) FROM employe;
    END IF;
END $$;
COMMIT;
-- Au redemarrage, EmployeInitializer insere les employes des contrats existants.
-- Un dossier cv_candidat correspondant au candidat ET a l'offre est obligatoire.
