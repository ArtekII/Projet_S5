package com.project.repository;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.*;

class DossierAdministratifMigrationTest {
    @Test
    void migrationReexecutableConserveFichesDocumentsEtJournal() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migration-dossier;MODE=PostgreSQL", "sa", "")) {
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("create table utilisateur (id int primary key)");
            jdbc.update("insert into utilisateur values (1), (2)");
            var script = new FileSystemResource("sql/migrations/20261108_dossier_administratif.sql");
            ScriptUtils.executeSqlScript(connection, script);
            jdbc.update("""
                    insert into dossier_administratif (id_utilisateur, nom, prenom, date_naissance, adresse,
                    telephone, email_personnel, situation_matrimoniale)
                    values (1, 'Rakoto', 'Onja', '1995-01-02', 'Antananarivo', '0341234567', 'onja@example.com', 'CELIBATAIRE')
                    """);
            jdbc.update("insert into document_administratif (id_utilisateur, id_deposant, type, nom_fichier, type_mime, contenu, depose_le) values (1, 2, 'CV', 'cv.pdf', 'application/pdf', ?, current_timestamp)", new byte[]{1, 2, 3});
            jdbc.update("insert into journal_dossier (id_utilisateur, id_acteur, action, detail, date_action) values (1, 2, 'DEPOT_DOCUMENT', 'CV', current_timestamp)");
            ScriptUtils.executeSqlScript(connection, script);
            assertEquals(1, jdbc.queryForObject("select count(*) from dossier_administratif", Integer.class));
            assertEquals(1, jdbc.queryForObject("select count(*) from document_administratif", Integer.class));
            assertEquals(1, jdbc.queryForObject("select count(*) from journal_dossier", Integer.class));
            assertFalse(jdbc.queryForObject("select signature from dossier_administratif where id_utilisateur = 1", Boolean.class));
            assertArrayEquals(new byte[]{1, 2, 3}, jdbc.queryForObject("select contenu from document_administratif", byte[].class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("insert into parcours_professionnel (id_utilisateur, type, libelle, date_debut, montant, cree_le) values (1, 'SALAIRE', 'Salaire', '2026-11-08', -1, current_timestamp)"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("insert into document_administratif (id_utilisateur, id_deposant, type, nom_fichier, type_mime, contenu, date_delivrance, date_expiration, depose_le) values (1, 2, 'CV', 'cv.pdf', 'application/pdf', ?, '2026-11-08', '2026-11-07', current_timestamp)", new byte[]{1}));
        }
    }
}
