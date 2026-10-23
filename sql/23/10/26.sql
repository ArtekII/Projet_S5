INSERT INTO utilisateur (email, mot_de_passe, id_role, id_departement) VALUES
 ('test@demo.com',  '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='CANDIDAT'), NULL);
