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
