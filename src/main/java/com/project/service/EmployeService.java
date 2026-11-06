package com.project.service;

import com.project.entity.Contrat;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeService {
    public static class DossierCvManquantException extends IllegalStateException {
        public DossierCvManquantException(Integer candidatureId) {
            super("Dossier CV du candidat introuvable pour cette offre (candidature " + candidatureId + ").");
        }
    }
    private final JdbcTemplate jdbc;

    public EmployeService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void assurerPourContrat(Contrat contrat) {
        // Serialise aussi deux embauches du meme utilisateur sur des offres differentes.
        var dossier = jdbc.queryForMap("""
                select u.id, c.id_offre from candidature c
                join utilisateur u on u.id = c.id_candidat
                where c.id = ? for update of u
                """, contrat.getIdCandidature());
        Integer utilisateur = ((Number) dossier.get("id")).intValue();
        if (jdbc.queryForObject("select count(*) from employe where id_utilisateur = ?", Integer.class, utilisateur) > 0) return;
        var cvs = jdbc.queryForList("select id from cv_candidat where id_candidat = ? and id_offre = ?",
                Integer.class, utilisateur, dossier.get("id_offre"));
        if (cvs.size() != 1) throw new DossierCvManquantException(contrat.getIdCandidature());
        LocalDate date = contrat.getDateDebut() != null ? contrat.getDateDebut()
                : contrat.getCreatedAt().toLocalDate();
        jdbc.update("""
                insert into employe (id_utilisateur, id_cv_candidat, id_contrat_actuel, date_embauche)
                values (?, ?, ?, ?)
                """, utilisateur, cvs.get(0), contrat.getId(), date);
    }
}
