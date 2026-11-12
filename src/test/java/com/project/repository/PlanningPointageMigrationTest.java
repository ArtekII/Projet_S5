package com.project.repository;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.*;

class PlanningPointageMigrationTest {
    @Test void migrationReexecutableConserveLesPointagesEtLesReleves() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migration-pointage;MODE=PostgreSQL", "sa", "")) {
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("create table utilisateur (id int primary key)");
            jdbc.execute("create table poste (id int primary key)");
            jdbc.execute("create table employe (id_employe int primary key)");
            jdbc.update("insert into utilisateur values (1)"); jdbc.update("insert into poste values (1)"); jdbc.update("insert into employe values (1)");
            var migration = new FileSystemResource("sql/migrations/20261108_planning_pointage.sql");
            ScriptUtils.executeSqlScript(connection, migration);
            jdbc.update("insert into planning (id_poste, jour_semaine, date_effet, jour_travaille, heure_debut, heure_fin, pause_debut, pause_fin, id_rh, cree_le) values (1, 1, '2026-11-09', true, '08:00', '17:00', '12:00', '13:00', 1, current_timestamp)");
            jdbc.update("insert into pointage (id_employe, id_planning, id_poste, date_pointage, heure_connexion, heure_deconnexion, jeton) values (1, 1, 1, '2026-11-09', '2026-11-09 08:00:00', '2026-11-09 17:00:00', 'session-test')");
            jdbc.update("insert into variable_paie_pointage (id_employe, id_planning, date_pointage, prevu_secondes, travail_secondes, retard_secondes, supplementaires_secondes, absence_secondes, id_rh, transmis_le) values (1, 1, '2026-11-09', 28800, 28800, 0, 0, 0, 1, current_timestamp)");
            jdbc.update("insert into alerte_pointage (id_employe, date_pointage, code, message, detectee_le) values (1, '2026-11-09', 'RETARD', 'Retard', current_timestamp)");
            ScriptUtils.executeSqlScript(connection, migration);
            assertEquals(1, jdbc.queryForObject("select count(*) from planning", Integer.class));
            assertEquals(1, jdbc.queryForObject("select count(*) from pointage", Integer.class));
            assertEquals(28800L, jdbc.queryForObject("select travail_secondes from variable_paie_pointage", Long.class));
            assertEquals(1, jdbc.queryForObject("select count(*) from alerte_pointage", Integer.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("insert into planning (id_poste, jour_semaine, date_effet, jour_travaille, id_rh, cree_le) values (1, 8, '2026-11-09', false, 1, current_timestamp)"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("insert into variable_paie_pointage (id_employe, id_planning, date_pointage, prevu_secondes, travail_secondes, retard_secondes, supplementaires_secondes, absence_secondes, id_rh, transmis_le) values (1, 1, '2026-11-10', 28800, -1, 0, 0, 0, 1, current_timestamp)"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("insert into pointage (id_employe, date_pointage, heure_connexion, jeton) values (1, '2026-11-09', current_timestamp, 'session-test')"));
        }
    }
}
