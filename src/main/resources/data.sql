-- Donnees de reference necessaires a l'application.
INSERT INTO role (code, libelle)
SELECT v.code, v.libelle
FROM (VALUES
 ('MANAGER', 'Manager'),
 ('RH', 'Ressources humaines'),
 ('FINANCE', 'Bureau de finances'),
 ('DIRECTION', 'Direction'),
 ('CANDIDAT', 'Candidat')
) AS v(code, libelle)
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.code = v.code);

INSERT INTO decision (code, libelle)
SELECT v.code, v.libelle
FROM (VALUES
 ('EN_ATTENTE', 'En attente'),
 ('VALIDEE', 'Validee'),
 ('REFUSEE', 'Refusee')
) AS v(code, libelle)
WHERE NOT EXISTS (SELECT 1 FROM decision d WHERE d.code = v.code);

INSERT INTO statut_offre (code, libelle)
SELECT v.code, v.libelle
FROM (VALUES
 ('EN_ATTENTE_RH', 'En attente de validation RH'),
 ('REFUSEE_RH', 'Refusee par le RH'),
 ('EN_ATTENTE_FINANCE', 'En attente de validation Finance'),
 ('REFUSEE_FINANCE', 'Refusee par la Finance'),
 ('EN_ATTENTE_DIRECTION', 'En attente de validation Direction'),
 ('REFUSEE_DIRECTION', 'Refusee par la Direction'),
 ('PUBLIEE', 'Publiee'),
 ('CLOTUREE', 'Cloturee')
) AS v(code, libelle)
WHERE NOT EXISTS (SELECT 1 FROM statut_offre s WHERE s.code = v.code);

INSERT INTO departement (nom)
SELECT v.nom
FROM (VALUES
 ('Informatique'),
 ('Ressources Humaines'),
 ('Finance'),
 ('Direction'),
 ('Marketing')
) AS v(nom)
WHERE NOT EXISTS (SELECT 1 FROM departement d WHERE d.nom = v.nom);

INSERT INTO poste (intitule, description, actif)
SELECT v.intitule, v.description, v.actif
FROM (VALUES
 ('Developpeur Junior', 'Developpeur debutant Java/Spring', true),
 ('Developpeur Senior', 'Developpeur experimente Java/Spring', true),
 ('Chef de projet', 'Gestion de projets IT', true),
 ('Comptable', 'Gestion comptable et finance', true),
 ('Designer UI/UX', 'Conception interfaces utilisateur', true)
) AS v(intitule, description, actif)
WHERE NOT EXISTS (SELECT 1 FROM poste p WHERE p.intitule = v.intitule);

-- mot de passe = "password"  BCrypt $2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG
INSERT INTO utilisateur (email, mot_de_passe, id_role, id_departement)
SELECT v.email, v.mot_de_passe, r.id, d.id
FROM (VALUES
 ('manager@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', 'MANAGER', 'Informatique'),
 ('rh@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', 'RH', 'Ressources Humaines'),
 ('finance@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', 'FINANCE', 'Finance'),
 ('direction@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', 'DIRECTION', 'Direction'),
 ('candidat@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', 'CANDIDAT', NULL)
) AS v(email, mot_de_passe, role_code, departement_nom)
JOIN role r ON r.code = v.role_code
LEFT JOIN departement d ON d.nom = v.departement_nom
WHERE NOT EXISTS (SELECT 1 FROM utilisateur u WHERE u.email = v.email);
