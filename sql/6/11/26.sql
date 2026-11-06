-- =====================================================
-- Exemple de schéma : gestion des congés (SIRH)
-- Syntaxe compatible PostgreSQL (adaptable MySQL / SQL Server)
-- =====================================================

-- 0. Table de référence minimale (existe déjà dans un vrai SIRH)
CREATE TABLE employe (
    id_employe      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_utilisateur   INT NOT NULL UNIQUE REFERENCES utilisateur(id),
    id_cv_candidat INT NOT NULL REFERENCES cv_candidat(id),
    id_contrat_actuel    INT NOT NULL UNIQUE REFERENCES contrat(id),
    date_embauche  DATE NOT NULL
);

-- 1. TYPE DE CONGÉ : référentiel (CP, RTT, maladie, sans solde...)
CREATE TABLE type_conge (
    id_type_conge       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                VARCHAR(20)  NOT NULL UNIQUE,   -- ex : CP, RTT, MAL
    libelle             VARCHAR(100) NOT NULL,          -- ex : Congés payés
    est_remunere        BOOLEAN      NOT NULL DEFAULT TRUE,
    decompte_solde      BOOLEAN      NOT NULL DEFAULT TRUE,  -- consomme un solde ?
    acquisition_mensuelle DECIMAL(4,2),                 -- ex : 2.50 jours / mois
    actif               BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 2. CONGÉ : les demandes / congés posés
CREATE TABLE conge (
    id_conge        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_employe      INT  NOT NULL REFERENCES employe(id_employe),
    id_type_conge   INT  NOT NULL REFERENCES type_conge(id_type_conge),
    date_debut      DATE NOT NULL,
    date_fin        DATE NOT NULL,
    nb_jours        DECIMAL(5,2) NOT NULL,              -- jours décomptés
    statut          VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE'
                    CHECK (statut IN ('EN_ATTENTE','VALIDE','REFUSE','ANNULE')),
    commentaire     VARCHAR(500),
    date_demande    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_decision   TIMESTAMP,
    CHECK (date_fin >= date_debut),
    CHECK (nb_jours > 0)
);

-- 3. SOLDE DE CONGÉ : un solde par employé, type et période d'acquisition
CREATE TABLE solde_conge (
    id_solde        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_employe      INT NOT NULL REFERENCES employe(id_employe),
    id_type_conge   INT NOT NULL REFERENCES type_conge(id_type_conge),
    periode_debut   DATE NOT NULL,                      -- ex : 2026-06-01
    periode_fin     DATE NOT NULL,                      -- ex : 2027-05-31
    jours_acquis    DECIMAL(5,2) NOT NULL DEFAULT 0,
    jours_pris      DECIMAL(5,2) NOT NULL DEFAULT 0,
    -- solde disponible calculé
    solde_disponible DECIMAL(5,2)
        GENERATED ALWAYS AS (jours_acquis - jours_pris) STORED,
    date_maj        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_employe, id_type_conge, periode_debut)
);

-- 4. HISTORIQUE : journal de tout ce qui arrive aux congés (audit / traçabilité)
CREATE TABLE historique_conge (
    id_historique   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conge        INT NOT NULL REFERENCES conge(id_conge),
    id_employe      INT NOT NULL REFERENCES employe(id_employe),
    action          VARCHAR(20) NOT NULL
                    CHECK (action IN ('CREATION','MODIFICATION','VALIDATION',
                                      'REFUS','ANNULATION','PRISE_EFFECTIVE')),
    ancien_statut   VARCHAR(20),
    nouveau_statut  VARCHAR(20),
    nb_jours        DECIMAL(5,2),                       -- jours concernés au moment de l'action
    solde_avant     DECIMAL(5,2),
    solde_apres     DECIMAL(5,2),
    id_auteur       INT NOT NULL REFERENCES employe(id_employe),  -- qui a fait l'action
    date_action     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    commentaire     VARCHAR(500)
);

-- Index utiles
CREATE INDEX idx_conge_employe_dates ON conge (id_employe, date_debut, date_fin);
CREATE INDEX idx_conge_statut        ON conge (statut);
CREATE INDEX idx_hist_conge          ON historique_conge (id_conge, date_action);

-- =====================================================
-- Exemple de données
-- =====================================================
-- INSERT INTO type_conge VALUES (1,'CP','Congés payés',TRUE,TRUE,2.50,TRUE);
-- INSERT INTO type_conge VALUES (2,'RTT','Réduction du temps de travail',TRUE,TRUE,NULL,TRUE);
-- INSERT INTO type_conge VALUES (3,'MAL','Arrêt maladie',TRUE,FALSE,NULL,TRUE);

-- Exemple de requête : congés validés d'un employé avec son solde
-- SELECT c.date_debut, c.date_fin, c.nb_jours, t.libelle, s.solde_disponible
-- FROM conge c
-- JOIN type_conge t ON t.id_type_conge = c.id_type_conge
-- JOIN solde_conge s ON s.id_employe = c.id_employe
--                   AND s.id_type_conge = c.id_type_conge
--                   AND c.date_debut BETWEEN s.periode_debut AND s.periode_fin
-- WHERE c.id_employe = 1 AND c.statut = 'VALIDE';