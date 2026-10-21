package com.project.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Acces aux candidatures du schema 26.sql, qui ne sont pas encore mappees en JPA. */
@Repository
public class CandidatureRepository {
    private final JdbcTemplate jdbc;

    public CandidatureRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> findPourContratForUpdate(Integer candidatureId, Integer candidatId) {
        return jdbc.queryForList("""
                select c.cdi_autorise, c.id_candidat as candidatid, o.id_poste as posteid, e.code as etape, s.code as statut
                from candidature c join offre_emploi o on o.id = c.id_offre
                join etape_recrutement e on e.id = c.id_etape
                join statut_candidature s on s.id = c.id_statut
                where c.id = ? and c.id_candidat = ? for update of c
                """, candidatureId, candidatId).stream().findFirst();
    }

    public Map<String, Object> verrouiller(Integer candidatureId) {
        return jdbc.queryForList("""
                select c.id, c.id_candidat as candidatid, c.cdi_autorise,
                    e.code as etape, s.code as statut
                from candidature c
                join etape_recrutement e on e.id = c.id_etape
                join statut_candidature s on s.id = c.id_statut
                where c.id = ? for update of c
                """, candidatureId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
    }

    public void autoriserCdi(Integer candidatureId, Integer rhId) {
        jdbc.update("update candidature set cdi_autorise = true, id_rh_cdi = ? where id = ?", rhId, candidatureId);
    }

    public void refuserParCandidat(Integer candidatureId) {
        int lignes = jdbc.update("""
                update candidature set id_statut =
                    (select id from statut_candidature where code = 'REFUSE_PAR_CANDIDAT')
                where id = ?
                """, candidatureId);
        if (lignes != 1) throw new IllegalStateException("Candidature introuvable");
    }

    public void embaucher(Integer candidatureId) {
        int lignes = jdbc.update("""
                update candidature set id_etape = (select id from etape_recrutement where code = 'EMBAUCHE'),
                    id_statut = (select id from statut_candidature where code = 'EMBAUCHE')
                where id = ?
                """, candidatureId);
        if (lignes != 1) throw new IllegalStateException("Candidature introuvable");
    }

    public List<Map<String, Object>> findDossiersConditions(Integer candidatId) {
        String sql = """
                select c.id, c.cdi_autorise, p.id as posteid, p.intitule, u.email
                from candidature c join offre_emploi o on o.id = c.id_offre
                join poste p on p.id = o.id_poste join utilisateur u on u.id = c.id_candidat
                join etape_recrutement e on e.id = c.id_etape
                join statut_candidature s on s.id = c.id_statut
                where e.code = 'CONDITIONS' and s.code = 'EN_COURS'
                  and not exists (select 1 from contrat co where co.id_candidature = c.id)
                """;
        return candidatId == null
                ? jdbc.queryForList(sql + " order by c.id desc")
                : jdbc.queryForList(sql + " and c.id_candidat = ? order by c.id desc", candidatId);
    }

    // Appelee dans une transaction : le verrou protege la premiere proposition individuelle.
    public Optional<Integer> findPostePourNegociationForUpdate(Integer candidatureId, Integer candidatId) {
        return jdbc.queryForList("""
                select o.id_poste from candidature c join offre_emploi o on o.id = c.id_offre
                join etape_recrutement e on e.id = c.id_etape
                join statut_candidature s on s.id = c.id_statut
                where c.id = ? and c.id_candidat = ? and e.code = 'CONDITIONS' and s.code = 'EN_COURS'
                  and not exists (select 1 from contrat co where co.id_candidature = c.id)
                for update of c
                """, Integer.class, candidatureId, candidatId).stream().findFirst();
    }
}
