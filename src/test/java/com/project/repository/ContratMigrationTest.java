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
    void migrationReprendLesAnciensBrouillonsSansRouvrirLesDossiersClos() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migration;MODE=PostgreSQL", "sa", "")) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("create table utilisateur (id int primary key)");
            jdbc.execute("create table etape_recrutement (id int primary key, code varchar(20))");
            jdbc.execute("create table statut_candidature (id int primary key, code varchar(30))");
            jdbc.execute("create table candidature (id int primary key, id_etape int, id_statut int)");
            jdbc.execute("create table condition_travail (id int primary key)");
            jdbc.execute("create table contrat (id int primary key, id_candidature int)");
            jdbc.update("insert into etape_recrutement values (4, 'CONDITIONS'), (5, 'EMBAUCHE')");
            jdbc.update("insert into statut_candidature values (1, 'EN_COURS'), (4, 'EMBAUCHE')");
            jdbc.update("insert into candidature values (100, 5, 1), (200, 5, 4)");
            jdbc.update("insert into contrat values (1, 100), (2, 200)");
            var script = new FileSystemResource("sql/migrations/20261021_contrats_delais.sql");
            ScriptUtils.executeSqlScript(connection, script);
            ScriptUtils.executeSqlScript(connection, script);
            assertEquals(4, jdbc.queryForObject("select id_etape from candidature where id = 100", Integer.class));
            assertEquals(5, jdbc.queryForObject("select id_etape from candidature where id = 200", Integer.class));
            assertEquals("BROUILLON", jdbc.queryForObject("select statut from contrat where id = 1", String.class));
        }
    }
}
