-- ATTENTION : efface toutes les donnees des tables du schema public.
-- Conserve les tables, contraintes et vues. Arreter l'application avant execution.
BEGIN;
DO $$
DECLARE
    tables_a_vider text;
BEGIN
    SELECT string_agg(format('%I.%I', schemaname, tablename), ', ')
    INTO tables_a_vider
    FROM pg_tables
    WHERE schemaname = 'public';

    IF tables_a_vider IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || tables_a_vider || ' RESTART IDENTITY CASCADE';
    END IF;
END $$;
COMMIT;
