-- PostgreSQL : donnees de candidats retenus apres la troisieme etape.
-- Mot de passe des trois candidats : Test1234!
-- Reexecution sans doublons, sans effacer les reponses deja donnees.
BEGIN;
DO $$
DECLARE
    poste_id integer;
    manager_id integer;
    rh_id integer;
    departement_id integer;
    offre_id integer;
    candidat_id integer;
    cv_id integer;
    candidature_id integer;
    candidat record;
BEGIN
    SELECT id INTO STRICT poste_id FROM poste WHERE intitule = 'Developpeur Junior';
    SELECT id, id_departement INTO STRICT manager_id, departement_id FROM utilisateur WHERE email = 'manager@demo.com';
    SELECT id INTO STRICT rh_id FROM utilisateur WHERE email = 'rh@demo.com';
    SELECT id INTO offre_id FROM offre_emploi
    WHERE id_poste = poste_id AND id_manager = manager_id
      AND nb_places = 3 AND created_at = TIMESTAMP '2026-10-22 12:00:00';
    IF offre_id IS NULL THEN
        INSERT INTO offre_emploi (id_poste, id_departement, id_manager, nb_places, salaire, id_statut, date_publication, created_at)
        VALUES (poste_id, departement_id, manager_id, 3, 1500000,
                (SELECT id FROM statut_offre WHERE code = 'PUBLIEE'), CURRENT_TIMESTAMP, TIMESTAMP '2026-10-22 12:00:00')
        RETURNING id INTO offre_id;
    END IF;
    INSERT INTO condition_travail (id_poste, version, id_role_proposant, salaire, heures_par_semaine, horaires, lieu_travail, id_decision_rh)
    SELECT poste_id, 1, (SELECT id FROM role WHERE code = 'RH'), 1500000, 40,
           'Lundi au vendredi, 8 h a 17 h', 'Antananarivo', (SELECT id FROM decision WHERE code = 'EN_ATTENTE')
    WHERE NOT EXISTS (SELECT 1 FROM condition_travail WHERE id_poste = poste_id);
    FOR candidat IN SELECT * FROM (VALUES
        ('alice@demo.com', 'Rakoto', 'Alice', 18),
        ('bob@demo.com', 'Rabe', 'Bob', 17),
        ('clara@demo.com', 'Andria', 'Clara', 16)
    ) AS donnees(email, nom, prenom, note)
    LOOP
        INSERT INTO utilisateur (email, mot_de_passe, id_role)
        SELECT candidat.email, '$2a$10$j7PYAMkVM9NfRNegNCbcC.jOBno1H1I1O1rjBPGgftA3KiQAvVmgK', id FROM role WHERE code = 'CANDIDAT'
        ON CONFLICT (email) DO NOTHING;
        SELECT id INTO STRICT candidat_id FROM utilisateur WHERE email = candidat.email;
        SELECT id INTO candidature_id FROM candidature WHERE id_offre = offre_id AND id_candidat = candidat_id;
        IF candidature_id IS NULL THEN
            INSERT INTO cv (id_candidat, nom, prenom, date_naissance, diplome, competences)
            VALUES (candidat_id, candidat.nom, candidat.prenom, DATE '1998-01-15', 'Licence informatique', 'Java, Spring, PostgreSQL')
            RETURNING id INTO cv_id;
            INSERT INTO candidature (id_offre, id_candidat, id_cv, id_etape, id_statut)
            VALUES (offre_id, candidat_id, cv_id, (SELECT id FROM etape_recrutement WHERE code = 'ENTRETIEN'),
                    (SELECT id FROM statut_candidature WHERE code = 'EN_COURS'))
            RETURNING id INTO candidature_id;
            INSERT INTO entretien (id_candidature, id_rh, date_entretien, note_honnetete_cv, note_personnalite, note_competences)
            VALUES (candidature_id, rh_id, CURRENT_TIMESTAMP, candidat.note, candidat.note, candidat.note);
        END IF;
    END LOOP;
END $$;
COMMIT;
