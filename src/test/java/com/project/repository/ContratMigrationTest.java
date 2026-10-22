package com.project.repository;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import static org.junit.jupiter.api.Assertions.*;

class ContratMigrationTest {
    @Test
    void migrationFinaliseLesContratsPreparesSansRouvrirLesDossiersClos() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migration;MODE=PostgreSQL", "sa", "")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("create table utilisateur (id int primary key)");
            jdbc.execute("create table etape_recrutement (id int primary key, code varchar(20))");
            jdbc.execute("create table statut_candidature (id int primary key, code varchar(30))");
            jdbc.execute("create table candidature (id int primary key, id_etape int, id_statut int)");
            jdbc.execute("create table condition_travail (id int primary key)");
            jdbc.execute("create table contrat (id int primary key, id_candidature int, statut varchar(20), signe_le timestamp, id_signataire int, revision bigint, constraint contrat_statut_check check (statut in ('BROUILLON', 'A_SIGNER', 'SIGNE')))");
            jdbc.update("insert into etape_recrutement values (4, 'CONDITIONS'), (5, 'EMBAUCHE')");
            jdbc.update("insert into statut_candidature values (1, 'EN_COURS'), (3, 'REFUSE_PAR_CANDIDAT'), (4, 'EMBAUCHE')");
            jdbc.update("insert into candidature values (100, 4, 1), (200, 4, 3), (300, 4, 1), (400, 5, 4)");
            jdbc.update("insert into contrat values (1, 100, 'A_SIGNER', null, null, 1), (2, 200, 'A_SIGNER', null, null, 1), (3, 300, 'BROUILLON', null, null, 0), (4, 400, 'SIGNE', current_timestamp, 400, 2)");
            var script = new FileSystemResource("sql/migrations/20261021_attribution_directe.sql");
            ScriptUtils.executeSqlScript(connection, script);
            ScriptUtils.executeSqlScript(connection, script);
            assertEquals(5, jdbc.queryForObject("select id_etape from candidature where id = 100", Integer.class));
            assertEquals(4, jdbc.queryForObject("select id_statut from candidature where id = 100", Integer.class));
            assertEquals(3, jdbc.queryForObject("select id_statut from candidature where id = 200", Integer.class));
            assertEquals(4, jdbc.queryForObject("select id_statut from candidature where id = 300", Integer.class));
            assertEquals("FINALISE", jdbc.queryForObject("select statut from contrat where id = 1", String.class));
            assertEquals("FINALISE", jdbc.queryForObject("select statut from contrat where id = 3", String.class));
            assertEquals("FINALISE", jdbc.queryForObject("select statut from contrat where id = 4", String.class));
            assertEquals(0, jdbc.queryForObject("select count(*) from information_schema.columns where table_name = 'CONTRAT' and column_name in ('SIGNE_LE', 'ID_SIGNATAIRE', 'REVISION')", Integer.class));
        }
    }
}
