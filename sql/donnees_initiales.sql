-- Donnees initiales extraites de schema.sql. Executer apres reset_donnees.sql.
BEGIN;
INSERT INTO role (code, libelle) VALUES
    ('MANAGER',   'Manager'),
    ('RH',        'Ressources humaines'),
    ('FINANCE',   'Bureau de finances'),
    ('DIRECTION', 'Direction'),
    ('CANDIDAT',  'Candidat');

INSERT INTO decision (code, libelle) VALUES
    ('EN_ATTENTE', 'En attente'),
    ('VALIDEE',    'Validee'),
    ('REFUSEE',    'Refusee');

INSERT INTO statut_offre (code, libelle) VALUES
    ('EN_ATTENTE_RH',        'En attente de validation RH'),
    ('REFUSEE_RH',           'Refusee par le RH'),
    ('EN_ATTENTE_FINANCE',   'En attente de validation Finance'),
    ('REFUSEE_FINANCE',      'Refusee par la Finance'),
    ('EN_ATTENTE_DIRECTION', 'En attente de validation Direction'),
    ('REFUSEE_DIRECTION',    'Refusee par la Direction'),
    ('PUBLIEE',              'Publiee'),
    ('CLOTUREE',             'Cloturee');

INSERT INTO etape_recrutement (code, libelle, ordre) VALUES
    ('CV',            'Selection des CV',  1),
    ('QUESTIONNAIRE', 'Questionnaire',     2),
    ('ENTRETIEN',     'Entretien',         3),
    ('CONDITIONS',    'Conditions de travail', 4),
    ('EMBAUCHE',      'Embauche',          5);

INSERT INTO statut_candidature (code, libelle) VALUES
    ('EN_COURS',            'En cours'),
    ('ELIMINE',             'Elimine'),
    ('REFUSE_PAR_CANDIDAT', 'Refuse par le candidat'),
    ('EMBAUCHE',            'Embauche');

INSERT INTO resultat (code, libelle) VALUES
    ('REUSSI', 'Reussi'),
    ('ECHOUE', 'Echoue');

INSERT INTO type_question (code, libelle) VALUES
    ('QCM',   'Choix multiple'),
    ('TEXTE', 'Reponse libre');

INSERT INTO reponse_condition (code, libelle) VALUES
    ('OUI',   'Accepte'),
    ('NON',   'Refuse'),
    ('AUTRE', 'Autre remarque / modification');

INSERT INTO type_contrat (code, libelle) VALUES
    ('CE',  'Contrat d''essai'),
    ('CDI', 'Contrat a duree indeterminee');

INSERT INTO type_notification (code, libelle) VALUES
    ('SELECTION_CV', 'Selection du CV'),
    ('QUESTIONNAIRE', 'Questionnaire'),
    ('ENTRETIEN',    'Entretien'),
    ('CLASSEMENT',   'Classement'),
    ('CONDITIONS',   'Conditions de travail'),
    ('OFFRE',        'Offre d''emploi'),
    ('INFO',         'Information');

INSERT INTO etape (nom) VALUES
    ('selection'),
    ('Test'),
    ('Entretien');

INSERT INTO departement (nom) VALUES ('Informatique'), ('Ressources Humaines'), ('Finance'), ('Direction'), ('Marketing');

INSERT INTO poste (intitule, description, actif) VALUES
 ('Developpeur Junior', 'Developpeur debutant Java/Spring', true),
 ('Developpeur Senior', 'Developpeur experimente Java/Spring', true),
 ('Chef de projet', 'Gestion de projets IT', true),
 ('Comptable', 'Gestion comptable et finance', true),
 ('Designer UI/UX', 'Conception interfaces utilisateur', true);

INSERT INTO utilisateur (email, mot_de_passe, id_role, id_departement) VALUES
 ('manager@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='MANAGER'), (SELECT id FROM departement WHERE nom='Informatique')),
 ('rh@demo.com',        '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='RH'), (SELECT id FROM departement WHERE nom='Ressources Humaines')),
 ('finance@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='FINANCE'), (SELECT id FROM departement WHERE nom='Finance')),
 ('direction@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='DIRECTION'), (SELECT id FROM departement WHERE nom='Direction')),
 ('candidat@demo.com',  '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='CANDIDAT'), NULL);
COMMIT;
