CREATE TABLE departement (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE role (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

-- Decision d'un valideur (demande de poste, offre, conditions de travail)
CREATE TABLE decision (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE statut_offre (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE etape_recrutement (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL,
    ordre    INT NOT NULL UNIQUE
);

CREATE TABLE statut_candidature (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE resultat (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE type_question (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

-- Reponse du candidat a la proposition de conditions de travail
CREATE TABLE reponse_condition (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE type_contrat (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE type_notification (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

-- ---- Donnees de reference ----
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

-- ---------------------------------------------------------------------
-- UTILISATEURS
-- ---------------------------------------------------------------------
CREATE TABLE utilisateur (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(255) NOT NULL,              -- hash (bcrypt/argon2)
    id_role         INT NOT NULL REFERENCES role(id),
    id_departement  INT REFERENCES departement(id),
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- POSTES ET DEMANDES DE NOUVEAUX POSTES
-- ---------------------------------------------------------------------

-- Catalogue des postes (contenu du dropdown du manager)
CREATE TABLE poste (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    intitule    VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    actif       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE TABLE historique_poste (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_poste         INT NOT NULL REFERENCES poste(id),
    id_utilisateur   INT NOT NULL REFERENCES utilisateur(id),
    actif            BOOLEAN NOT NULL,
    date_modification TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Demande du manager pour un poste pas présent encore
CREATE TABLE demande_poste (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_manager       INT NOT NULL REFERENCES utilisateur(id),
    id_departement   INT NOT NULL REFERENCES departement(id),
    intitule_propose VARCHAR(150) NOT NULL,
    justification    TEXT,
    id_decision      INT NOT NULL DEFAULT 1 REFERENCES decision(id),   -- 1 = EN_ATTENTE
    id_rh            INT REFERENCES utilisateur(id),
    motif_refus      TEXT,
    id_poste         INT REFERENCES poste(id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    decided_at       TIMESTAMP
);

-- ---------------------------------------------------------------------
--  OFFRES D'EMPLOI ET CIRCUIT DE VALIDATION
-- ---------------------------------------------------------------------

CREATE TABLE offre_emploi (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_poste            INT NOT NULL REFERENCES poste(id),
    id_departement      INT NOT NULL REFERENCES departement(id),
    id_manager          INT NOT NULL REFERENCES utilisateur(id),
    annees_experience   INT NOT NULL DEFAULT 0 CHECK (annees_experience >= 0),
    nb_places           INT NOT NULL CHECK (nb_places > 0),
    salaire             NUMERIC(12,2) CHECK (salaire IS NULL OR salaire >= 0),  -- NULL tant que le RH ne l'a pas fixe
    id_statut           INT NOT NULL DEFAULT 1 REFERENCES statut_offre(id),     -- 1 = EN_ATTENTE_RH
    date_publication    TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Historique de chaque decision. id_role_valideur = RH, FINANCE ou DIRECTION.
CREATE TABLE offre_validation (
    id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_offre           INT NOT NULL REFERENCES offre_emploi(id) ON DELETE CASCADE,
    id_role_valideur   INT NOT NULL REFERENCES role(id),
    id_valideur        INT NOT NULL REFERENCES utilisateur(id),
    id_decision        INT NOT NULL REFERENCES decision(id),
    salaire_avant      NUMERIC(12,2),
    salaire_apres      NUMERIC(12,2),                   -- different si la Finance modifie le salaire
    commentaire        TEXT,
    decided_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- CANDIDATURES
-- ---------------------------------------------------------------------

CREATE TABLE cv (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidat     INT NOT NULL REFERENCES utilisateur(id),
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    date_naissance  DATE NOT NULL,
    diplome         TEXT NOT NULL,
    competences     TEXT NOT NULL,
    hobby           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Version de depot utilisee par le portail pour relier un CV a une offre.
CREATE TABLE cv_candidat (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidat      INT NOT NULL REFERENCES utilisateur(id),
    id_offre         INT NOT NULL REFERENCES offre_emploi(id),
    nom              VARCHAR(255) NOT NULL,
    prenom           VARCHAR(255) NOT NULL,
    date_naissance   DATE,
    annee_experience INT NOT NULL DEFAULT 0,
    email            VARCHAR(255),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_candidat, id_offre)
);

-- Historique detaille des phases RH, partage avec l'espace candidat.
CREATE TABLE etape (
    id  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO etape (nom) VALUES
    ('selection'),
    ('Test'),
    ('Entretien');

CREATE TABLE candidats_selec (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_offre         INT NOT NULL REFERENCES offre_emploi(id) ON DELETE CASCADE,
    id_cv_candidat   INT NOT NULL REFERENCES cv_candidat(id) ON DELETE CASCADE,
    id_etape         INT NOT NULL REFERENCES etape(id),
    note             NUMERIC(5,2) DEFAULT 0,
    rang             INT,
    date_selection   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_offre, id_cv_candidat, id_etape)
);

CREATE TABLE candidature (
    id                    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_offre              INT NOT NULL REFERENCES offre_emploi(id),
    id_candidat           INT NOT NULL REFERENCES utilisateur(id),
    id_cv                 INT NOT NULL REFERENCES cv(id),
    id_etape              INT NOT NULL DEFAULT 1 REFERENCES etape_recrutement(id),   -- 1 = CV
    id_statut             INT NOT NULL DEFAULT 1 REFERENCES statut_candidature(id),  -- 1 = EN_COURS
    score_questionnaire   NUMERIC(6,2),
    cdi_autorise          BOOLEAN NOT NULL DEFAULT FALSE,   -- accreditation donnee par le RH
    id_rh_cdi             INT REFERENCES utilisateur(id),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_offre, id_candidat)
);

-- Historique du passage de chaque etape
CREATE TABLE candidature_etape (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature   INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    id_etape         INT NOT NULL REFERENCES etape_recrutement(id),
    id_resultat      INT NOT NULL REFERENCES resultat(id),
    id_decideur      INT REFERENCES utilisateur(id),        -- NULL si automatique
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- QUESTIONNAIRES
-- ---------------------------------------------------------------------

CREATE TABLE questionnaire (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_offre        INT NOT NULL UNIQUE REFERENCES offre_emploi(id) ON DELETE CASCADE,
    titre           VARCHAR(150) NOT NULL,
    seuil_reussite  NUMERIC(6,2) NOT NULL,
    id_createur     INT NOT NULL REFERENCES utilisateur(id)
);

CREATE TABLE question (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_questionnaire  INT NOT NULL REFERENCES questionnaire(id) ON DELETE CASCADE,
    id_type           INT NOT NULL DEFAULT 1 REFERENCES type_question(id),   -- 1 = QCM
    enonce            TEXT NOT NULL,
    points            NUMERIC(5,2) NOT NULL DEFAULT 1,
    ordre             INT NOT NULL DEFAULT 1
);

CREATE TABLE choix_reponse (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_question  INT NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    libelle      TEXT NOT NULL,
    est_correct  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE reponse_candidat (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature   INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    id_question      INT NOT NULL REFERENCES question(id),
    id_choix         INT REFERENCES choix_reponse(id),
    reponse_texte    TEXT,
    points_obtenus   NUMERIC(5,2),
    UNIQUE (id_candidature, id_question)
);

-- ---------------------------------------------------------------------
-- ENTRETIEN ET CLASSEMENT
-- ---------------------------------------------------------------------

CREATE TABLE entretien (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature      INT NOT NULL UNIQUE REFERENCES candidature(id) ON DELETE CASCADE,
    id_rh               INT NOT NULL REFERENCES utilisateur(id),
    date_entretien      TIMESTAMP,
    note_honnetete_cv   NUMERIC(4,2) NOT NULL CHECK (note_honnetete_cv BETWEEN 0 AND 20),
    note_personnalite   NUMERIC(4,2) NOT NULL CHECK (note_personnalite BETWEEN 0 AND 20),
    note_competences    NUMERIC(4,2) NOT NULL CHECK (note_competences BETWEEN 0 AND 20),
    note_totale         NUMERIC(5,2) GENERATED ALWAYS AS
                        (note_honnetete_cv + note_personnalite + note_competences) STORED
);

-- Rang durant anle entretien
CREATE VIEW v_classement AS
SELECT t.*,
       (t.rang <= t.nb_places) AS retenu
FROM (
    SELECT c.id_offre,
           c.id AS id_candidature,
           e.note_totale,
           o.nb_places,
           RANK() OVER (PARTITION BY c.id_offre ORDER BY e.note_totale DESC) AS rang
    FROM candidature c
    JOIN statut_candidature s ON s.id = c.id_statut AND s.code = 'EN_COURS'
    JOIN entretien e          ON e.id_candidature = c.id
    JOIN offre_emploi o       ON o.id = c.id_offre
) t;

-- ---------------------------------------------------------------------
-- CONDITIONS DE TRAVAIL ET CONTRAT
-- ---------------------------------------------------------------------

CREATE TABLE condition_travail (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature       INT REFERENCES candidature(id) ON DELETE CASCADE,
    id_poste             INT REFERENCES poste(id),
    version              INT NOT NULL DEFAULT 1,
    id_role_proposant    INT NOT NULL REFERENCES role(id),          -- RH ou CANDIDAT
    salaire              NUMERIC(12,2) NOT NULL CHECK (salaire >= 0),
    heures_par_semaine   NUMERIC(4,1) NOT NULL CHECK (heures_par_semaine > 0),
    horaires             VARCHAR(100),
    lieu_travail         VARCHAR(255) NOT NULL,
    id_reponse           INT REFERENCES reponse_condition(id),      -- NULL tant que le candidat n'a pas repondu
    remarque             TEXT,
    id_decision_rh       INT NOT NULL DEFAULT 1 REFERENCES decision(id),
    repondre_avant       TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_candidature, version),
    UNIQUE (id_poste, version),
    CHECK ((id_poste IS NULL) <> (id_candidature IS NULL))
);

CREATE TABLE contrat (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature    INT UNIQUE REFERENCES candidature(id),   -- NULL pour un contrat successif rattache a un employe
    id_employe        INT,                                      -- FK ajoutee apres creation de la table employe
    id_condition      INT NOT NULL REFERENCES condition_travail(id),
    id_type_contrat   INT NOT NULL DEFAULT 1 REFERENCES type_contrat(id),   -- 1 = CE
    date_debut        DATE,
    date_fin          DATE,                              -- NULL pour un CDI
    duree_mois        INT CHECK (duree_mois IS NULL OR duree_mois > 0),
    statut            VARCHAR(20) NOT NULL DEFAULT 'FINALISE'
                      CHECK (statut IN ('FINALISE', 'PROGRAMME')),
    id_rh_preparation INT REFERENCES utilisateur(id),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- NOTIFICATIONS (boite de reception)
-- ---------------------------------------------------------------------

CREATE TABLE notification (
    id                    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_destinataire       INT NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    id_type_notification  INT NOT NULL REFERENCES type_notification(id),
    titre                 VARCHAR(150) NOT NULL,
    message               TEXT NOT NULL,
    id_candidature        INT REFERENCES candidature(id) ON DELETE SET NULL,
    id_offre              INT REFERENCES offre_emploi(id) ON DELETE SET NULL,
    lue                   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);
-- ===========================================
-- Donnees ajoutees le 19/10/26 (run du projet)
-- ===========================================
INSERT INTO departement (nom) VALUES ('Informatique'), ('Ressources Humaines'), ('Finance'), ('Direction'), ('Marketing');
INSERT INTO poste (intitule, description, actif) VALUES
 ('Developpeur Junior', 'Developpeur debutant Java/Spring', true),
 ('Developpeur Senior', 'Developpeur experimente Java/Spring', true),
 ('Chef de projet', 'Gestion de projets IT', true),
 ('Comptable', 'Gestion comptable et finance', true),
 ('Designer UI/UX', 'Conception interfaces utilisateur', true);

-- mot de passe = "password"  BCrypt $2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG
INSERT INTO utilisateur (email, mot_de_passe, id_role, id_departement) VALUES
 ('manager@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='MANAGER'), (SELECT id FROM departement WHERE nom='Informatique')),
 ('rh@demo.com',        '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='RH'), (SELECT id FROM departement WHERE nom='Ressources Humaines')),
 ('finance@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='FINANCE'), (SELECT id FROM departement WHERE nom='Finance')),
 ('direction@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='DIRECTION'), (SELECT id FROM departement WHERE nom='Direction')),
 ('candidat@demo.com',  '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='CANDIDAT'), NULL);

CREATE TABLE employe (
    id_employe INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_utilisateur INT NOT NULL UNIQUE REFERENCES utilisateur(id),
    id_cv_candidat INT NOT NULL REFERENCES cv_candidat(id),
    id_contrat_actuel INT NOT NULL UNIQUE REFERENCES contrat(id),
    date_embauche DATE NOT NULL
);

ALTER TABLE contrat
    ADD CONSTRAINT fk_contrat_employe FOREIGN KEY (id_employe) REFERENCES employe(id_employe);

-- DOSSIER ADMINISTRATIF ET INTEGRATION
-- PostgreSQL : apres 20261108_contrats_employe.sql. Reexecutable sans effacer de donnees.
CREATE TABLE IF NOT EXISTS dossier_administratif (
    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_utilisateur INT NOT NULL UNIQUE REFERENCES utilisateur(id),
    revision BIGINT NOT NULL DEFAULT 0,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE NOT NULL,
    lieu_naissance VARCHAR(150),
    nationalite VARCHAR(100),
    numero_identite VARCHAR(100),
    adresse VARCHAR(500) NOT NULL,
    telephone VARCHAR(40) NOT NULL,
    email_personnel VARCHAR(255) NOT NULL,
    situation_matrimoniale VARCHAR(20) NOT NULL
        CHECK (situation_matrimoniale IN ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'UNION_LIBRE')),
    nombre_enfants INT CHECK (nombre_enfants BETWEEN 0 AND 50),
    contact_urgence VARCHAR(150),
    telephone_urgence VARCHAR(40),
    signature BOOLEAN NOT NULL DEFAULT FALSE,
    badge BOOLEAN NOT NULL DEFAULT FALSE,
    compte BOOLEAN NOT NULL DEFAULT FALSE,
    materiel BOOLEAN NOT NULL DEFAULT FALSE,
    formation BOOLEAN NOT NULL DEFAULT FALSE,
    paie BOOLEAN NOT NULL DEFAULT FALSE,
    modifie_le TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_administratif (
    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_utilisateur INT NOT NULL REFERENCES utilisateur(id),
    id_deposant INT NOT NULL REFERENCES utilisateur(id),
    type VARCHAR(30) NOT NULL CHECK (type IN ('CV', 'IDENTITE', 'CONTRAT', 'DIPLOME', 'ATTESTATION', 'AUTRE')),
    nom_fichier VARCHAR(255) NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    contenu BYTEA NOT NULL,
    date_delivrance DATE,
    date_expiration DATE,
    depose_le TIMESTAMP NOT NULL,
    CHECK (date_delivrance IS NULL OR date_expiration IS NULL OR date_expiration >= date_delivrance)
);
CREATE INDEX IF NOT EXISTS idx_document_utilisateur ON document_administratif(id_utilisateur);

CREATE TABLE IF NOT EXISTS parcours_professionnel (
    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_utilisateur INT NOT NULL REFERENCES utilisateur(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('AFFECTATION', 'SALAIRE', 'EXPERIENCE')),
    libelle VARCHAR(255) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE,
    montant NUMERIC(12,2) CHECK (montant >= 0),
    description VARCHAR(2000),
    cree_le TIMESTAMP NOT NULL,
    CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CHECK ((type = 'SALAIRE' AND montant IS NOT NULL) OR (type <> 'SALAIRE' AND montant IS NULL))
);
CREATE INDEX IF NOT EXISTS idx_parcours_utilisateur ON parcours_professionnel(id_utilisateur);

CREATE TABLE IF NOT EXISTS journal_dossier (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_utilisateur INT NOT NULL REFERENCES utilisateur(id),
    id_acteur INT NOT NULL REFERENCES utilisateur(id),
    action VARCHAR(40) NOT NULL,
    detail VARCHAR(1000) NOT NULL,
    date_action TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_journal_utilisateur ON journal_dossier(id_utilisateur, id);

-- BLOC 6 : PLANNING ET POINTAGE
-- Bloc 6 : appliquer apres les migrations employe, contrats et dossier administratif.
-- Reexecutable : aucun historique existant n'est efface.
CREATE TABLE IF NOT EXISTS planning (
    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_poste INT NOT NULL REFERENCES poste(id),
    jour_semaine INT NOT NULL CHECK (jour_semaine BETWEEN 1 AND 7),
    date_effet DATE NOT NULL,
    jour_travaille BOOLEAN NOT NULL,
    heure_debut TIME,
    heure_fin TIME,
    pause_debut TIME,
    pause_fin TIME,
    id_rh INT NOT NULL REFERENCES utilisateur(id),
    cree_le TIMESTAMP NOT NULL,
    CONSTRAINT uk_planning_poste_jour_effet UNIQUE (id_poste, jour_semaine, date_effet),
    CHECK ((jour_travaille AND heure_debut IS NOT NULL AND heure_fin IS NOT NULL AND heure_debut <> heure_fin
            AND ((pause_debut IS NULL AND pause_fin IS NULL) OR
                 (pause_debut IS NOT NULL AND pause_fin IS NOT NULL AND pause_debut <> pause_fin)))
        OR (NOT jour_travaille AND heure_debut IS NULL AND heure_fin IS NULL AND pause_debut IS NULL AND pause_fin IS NULL))
);

CREATE TABLE IF NOT EXISTS pointage (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    revision BIGINT NOT NULL DEFAULT 0,
    id_employe INT NOT NULL REFERENCES employe(id_employe),
    id_planning INT REFERENCES planning(id),
    id_poste INT REFERENCES poste(id),
    date_pointage DATE NOT NULL,
    heure_connexion TIMESTAMP NOT NULL,
    heure_deconnexion TIMESTAMP,
    connexion_retenue TIMESTAMP,
    deconnexion_retenue TIMESTAMP,
    regularise BOOLEAN NOT NULL DEFAULT FALSE,
    session_expiree BOOLEAN NOT NULL DEFAULT FALSE,
    jeton VARCHAR(36) NOT NULL UNIQUE,
    CHECK (NOT regularise OR (connexion_retenue IS NOT NULL AND deconnexion_retenue IS NOT NULL
                             AND deconnexion_retenue >= connexion_retenue))
);
CREATE INDEX IF NOT EXISTS idx_pointage_employe_date ON pointage(id_employe, date_pointage);

CREATE TABLE IF NOT EXISTS alerte_pointage (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_employe INT NOT NULL REFERENCES employe(id_employe),
    date_pointage DATE NOT NULL,
    code VARCHAR(40) NOT NULL,
    message VARCHAR(500) NOT NULL,
    detectee_le TIMESTAMP NOT NULL,
    resolue_le TIMESTAMP,
    CONSTRAINT uk_alerte_pointage_jour_code UNIQUE (id_employe, date_pointage, code)
);

CREATE TABLE IF NOT EXISTS correction_pointage (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_pointage BIGINT NOT NULL REFERENCES pointage(id),
    id_rh INT NOT NULL REFERENCES utilisateur(id),
    ancienne_entree TIMESTAMP,
    ancienne_sortie TIMESTAMP,
    nouvelle_entree TIMESTAMP NOT NULL,
    nouvelle_sortie TIMESTAMP NOT NULL,
    motif VARCHAR(1000) NOT NULL,
    corrige_le TIMESTAMP NOT NULL,
    CHECK (nouvelle_sortie >= nouvelle_entree)
);

CREATE TABLE IF NOT EXISTS variable_paie_pointage (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_employe INT NOT NULL REFERENCES employe(id_employe),
    id_planning INT NOT NULL REFERENCES planning(id),
    date_pointage DATE NOT NULL,
    prevu_secondes BIGINT NOT NULL CHECK (prevu_secondes >= 0),
    travail_secondes BIGINT NOT NULL CHECK (travail_secondes >= 0),
    retard_secondes BIGINT NOT NULL CHECK (retard_secondes >= 0),
    supplementaires_secondes BIGINT NOT NULL CHECK (supplementaires_secondes >= 0),
    absence_secondes BIGINT NOT NULL CHECK (absence_secondes >= 0),
    id_rh INT NOT NULL REFERENCES utilisateur(id),
    transmis_le TIMESTAMP NOT NULL,
    CONSTRAINT uk_variable_pointage_employe_date UNIQUE (id_employe, date_pointage)
);

-- BLOC 7 : CONGES
-- PostgreSQL : apres 20261108_conges.sql. Reexecutable sans effacer les donnees.
CREATE TABLE IF NOT EXISTS type_conge (
    id_type_conge INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    est_remunere BOOLEAN NOT NULL DEFAULT TRUE,
    decompte_solde BOOLEAN NOT NULL DEFAULT TRUE,
    acquisition_mensuelle DECIMAL(4,2)
        CHECK (acquisition_mensuelle IS NULL OR acquisition_mensuelle >= 0),
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS conge (
    id_conge INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_employe INT NOT NULL REFERENCES employe(id_employe),
    id_type_conge INT NOT NULL REFERENCES type_conge(id_type_conge),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nb_jours DECIMAL(5,2) NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut IN ('EN_ATTENTE', 'VALIDE', 'REFUSE', 'ANNULE')),
    commentaire VARCHAR(500),
    date_demande TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_decision TIMESTAMP,
    CHECK (date_fin >= date_debut),
    CHECK (nb_jours > 0)
);
CREATE INDEX IF NOT EXISTS idx_conge_employe_dates ON conge (id_employe, date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_conge_statut ON conge (statut);

CREATE TABLE IF NOT EXISTS solde_conge (
    id_solde INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_employe INT NOT NULL REFERENCES employe(id_employe),
    id_type_conge INT NOT NULL REFERENCES type_conge(id_type_conge),
    periode_debut DATE NOT NULL,
    periode_fin DATE NOT NULL,
    jours_acquis DECIMAL(5,2) NOT NULL DEFAULT 0,
    jours_pris DECIMAL(5,2) NOT NULL DEFAULT 0,
    solde_disponible DECIMAL(5,2)
        GENERATED ALWAYS AS (jours_acquis - jours_pris) STORED,
    date_maj TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_solde_conge UNIQUE (id_employe, id_type_conge, periode_debut),
    CHECK (periode_fin >= periode_debut),
    CHECK (jours_acquis >= 0),
    CHECK (jours_pris >= 0)
);

CREATE TABLE IF NOT EXISTS historique_conge (
    id_historique BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conge INT NOT NULL REFERENCES conge(id_conge),
    id_employe INT NOT NULL REFERENCES employe(id_employe),
    action VARCHAR(20) NOT NULL
        CHECK (action IN ('CREATION', 'MODIFICATION', 'VALIDATION', 'REFUS',
                          'ANNULATION', 'PRISE_EFFECTIVE')),
    ancien_statut VARCHAR(20),
    nouveau_statut VARCHAR(20),
    nb_jours DECIMAL(5,2),
    solde_avant DECIMAL(5,2),
    solde_apres DECIMAL(5,2),
    id_auteur INT REFERENCES utilisateur(id),
    date_action TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    commentaire VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_hist_conge ON historique_conge (id_conge, date_action);

INSERT INTO type_conge (code, libelle, est_remunere, decompte_solde, acquisition_mensuelle, actif)
VALUES
    ('CP', 'Congés payés', TRUE, TRUE, 2.50, TRUE),
    ('RTT', 'Réduction du temps de travail', TRUE, TRUE, 1.00, TRUE),
    ('MAL', 'Arrêt maladie', TRUE, FALSE, NULL, TRUE),
    ('SANS_SOLDE', 'Congé sans solde', FALSE, FALSE, NULL, TRUE)
ON CONFLICT (code) DO NOTHING;
