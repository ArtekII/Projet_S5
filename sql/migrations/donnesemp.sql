-- =====================================================================
-- Jeu de test : 4 employes, tous avec un contrat d'essai (CE)
-- PostgreSQL. A executer APRES la migration qui cree la table employe
-- et apres les donnees de reference (role, poste, utilisateur manager/rh...).
-- Mot de passe de tous les comptes crees = "password"
-- =====================================================================
BEGIN;

DO $$
DECLARE
    r           RECORD;
    v_mgr       INT;
    v_dept      INT;
    v_rh        INT;
    v_user      INT;
    v_cv        INT;
    v_offre     INT;
    v_cvc       INT;
    v_cand      INT;
    v_cond      INT;
    v_contrat   INT;
    v_emp       INT;
    v_hash      TEXT := '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG';
BEGIN
    SELECT id, id_departement INTO v_mgr, v_dept FROM utilisateur WHERE email = 'manager@demo.com';
    SELECT id INTO v_rh FROM utilisateur WHERE email = 'rh@demo.com';

    IF v_mgr IS NULL OR v_rh IS NULL THEN
        RAISE EXCEPTION 'Comptes manager@demo.com / rh@demo.com introuvables : executer d''abord les donnees de reference';
    END IF;

    FOR r IN
        SELECT * FROM (VALUES
            -- email, nom, prenom, naissance, poste, annees_exp, salaire, debut_CE, duree_mois, h/semaine
            ('emp1@demo.com', 'Rakoto',     'Andry',  DATE '1998-04-12', 'Developpeur Junior', 1, 1500000, DATE '2026-09-01', 3, 40),  -- CE qui se termine le 30/11/2026
            ('emp2@demo.com', 'Rasoa',      'Miora',  DATE '1995-09-03', 'Comptable',          3, 1800000, DATE '2026-10-01', 6, 40),  -- CE en cours jusqu'au 31/03/2027
            ('emp3@demo.com', 'Randria',    'Hery',   DATE '1999-01-27', 'Designer UI/UX',     2, 1600000, DATE '2026-07-01', 6, 35),  -- CE qui se termine le 31/12/2026
            ('emp4@demo.com', 'Razafindra', 'Fanja',  DATE '1992-11-15', 'Chef de projet',     5, 2500000, DATE '2026-11-02', 3, 40)   -- CE recent
        ) AS t(email, nom, prenom, naissance, poste, exp, salaire, debut, duree, heures)
    LOOP
        -- 1. Compte utilisateur (role CANDIDAT)
        INSERT INTO utilisateur (email, mot_de_passe, id_role, created_at)
        VALUES (r.email, v_hash, (SELECT id FROM role WHERE code = 'CANDIDAT'), NOW())
        RETURNING id INTO v_user;

        -- 2. CV
        INSERT INTO cv (id_candidat, nom, prenom, date_naissance, diplome, competences, hobby, created_at)
        VALUES (v_user, r.nom, r.prenom, r.naissance, 'Licence professionnelle',
                'Competences liees au poste ' || r.poste, 'Lecture, sport', NOW())
        RETURNING id INTO v_cv;

        -- 3. Offre (cloturee : le poste est pourvu)
        INSERT INTO offre_emploi (id_poste, id_departement, id_manager, annees_experience,
                                  nb_places, salaire, id_statut, date_publication, created_at)
        VALUES ((SELECT id FROM poste WHERE intitule = r.poste), v_dept, v_mgr, r.exp,
                1, r.salaire, (SELECT id FROM statut_offre WHERE code = 'CLOTUREE'),
                r.debut - INTERVAL '60 days', NOW())
        RETURNING id INTO v_offre;

        -- 4. Dossier candidat lie a l'offre (obligatoire pour l'employe)
        INSERT INTO cv_candidat (id_candidat, id_offre, nom, prenom, date_naissance, annee_experience, email, created_at)
        VALUES (v_user, v_offre, r.nom, r.prenom, r.naissance, r.exp, r.email, NOW())
        RETURNING id INTO v_cvc;

        -- 5. Candidature arrivee a l'etape EMBAUCHE
        INSERT INTO candidature (id_offre, id_candidat, id_cv, id_etape, id_statut, score_questionnaire, cdi_autorise, created_at)
        VALUES (v_offre, v_user, v_cv,
                (SELECT id FROM etape_recrutement WHERE code = 'EMBAUCHE'),
                (SELECT id FROM statut_candidature WHERE code = 'EMBAUCHE'),
                15.00, FALSE, NOW())
        RETURNING id INTO v_cand;

        -- 6. Conditions de travail acceptees par le candidat
        INSERT INTO condition_travail (id_candidature, version, id_role_proposant, salaire,
                                       heures_par_semaine, horaires, lieu_travail,
                                       id_reponse, id_decision_rh, created_at)
        VALUES (v_cand, 1, (SELECT id FROM role WHERE code = 'RH'), r.salaire,
                r.heures, '08:00 - 17:00', 'Antananarivo',
                (SELECT id FROM reponse_condition WHERE code = 'OUI'),
                (SELECT id FROM decision WHERE code = 'VALIDEE'), NOW())
        RETURNING id INTO v_cond;

        -- 7. Contrat CE
        INSERT INTO contrat (id_candidature, id_condition, id_type_contrat, date_debut, date_fin,
                             duree_mois, statut, id_rh_preparation, created_at)
        VALUES (v_cand, v_cond, (SELECT id FROM type_contrat WHERE code = 'CE'),
                r.debut, (r.debut + r.duree * INTERVAL '1 month' - INTERVAL '1 day')::DATE,
                r.duree, 'FINALISE', v_rh, NOW())
        RETURNING id INTO v_contrat;

        -- 8. Employe (id_employe genere par la sequence / identity)
        INSERT INTO employe (id_utilisateur, id_cv_candidat, id_contrat_actuel, date_embauche)
        VALUES (v_user, v_cvc, v_contrat, r.debut)
        RETURNING id_employe INTO v_emp;

        -- 9. Rattacher le contrat a l'employe
        UPDATE contrat SET id_employe = v_emp WHERE id = v_contrat;
    END LOOP;
END $$;

COMMIT;

-- ---------------------------------------------------------------------
-- Verification : tous les employes doivent avoir un contrat CE
-- ---------------------------------------------------------------------
SELECT e.id_employe, u.email, p.intitule AS poste, tc.code AS type_contrat,
       c.date_debut, c.date_fin, c.duree_mois
FROM employe e
JOIN utilisateur u   ON u.id = e.id_utilisateur
JOIN contrat c       ON c.id = e.id_contrat_actuel
JOIN type_contrat tc ON tc.id = c.id_type_contrat
JOIN candidature ca  ON ca.id = c.id_candidature
JOIN offre_emploi o  ON o.id = ca.id_offre
JOIN poste p         ON p.id = o.id_poste
ORDER BY e.id_employe;

-- ---------------------------------------------------------------------
-- Nettoyage (optionnel) : supprimer le jeu de test
-- ---------------------------------------------------------------------
-- BEGIN;
-- UPDATE contrat SET id_employe = NULL WHERE id_employe IN
--   (SELECT e.id_employe FROM employe e JOIN utilisateur u ON u.id = e.id_utilisateur WHERE u.email LIKE 'emp_@demo.com');
-- DELETE FROM employe WHERE id_utilisateur IN (SELECT id FROM utilisateur WHERE email LIKE 'emp_@demo.com');
-- DELETE FROM contrat WHERE id_candidature IN
--   (SELECT id FROM candidature WHERE id_candidat IN (SELECT id FROM utilisateur WHERE email LIKE 'emp_@demo.com'));
-- DELETE FROM condition_travail WHERE id_candidature IN
--   (SELECT id FROM candidature WHERE id_candidat IN (SELECT id FROM utilisateur WHERE email LIKE 'emp_@demo.com'));
-- DELETE FROM candidature WHERE id_candidat IN (SELECT id FROM utilisateur WHERE email LIKE 'emp_@demo.com');
-- DELETE FROM cv_candidat WHERE id_candidat IN (SELECT id FROM utilisateur WHERE email LIKE 'emp_@demo.com');
-- DELETE FROM offre_emploi WHERE id NOT IN (SELECT id_offre FROM candidature UNION SELECT id_offre FROM cv_candidat)
--   AND id_statut = (SELECT id FROM statut_offre WHERE code = 'CLOTUREE');
-- DELETE FROM cv WHERE id_candidat IN (SELECT id FROM utilisateur WHERE email LIKE 'emp_@demo.com');
-- DELETE FROM utilisateur WHERE email LIKE 'emp_@demo.com';
-- COMMIT;