package com.project.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardRhService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public DashboardRhService(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }

    /** Only the nearest deadlines are loaded; document contents are never selected. */
    public List<Map<String,Object>> prochainesEcheances() {
        var maintenant=LocalDateTime.now(clock);
        var aujourdHui=maintenant.toLocalDate();
        List<Map<String,Object>> echeances=new ArrayList<>();
        echeances.addAll(jdbc.query("""
            select e.date_entretien, u.email, p.intitule, o.id
            from entretien e join candidature c on c.id=e.id_candidature
            join statut_candidature s on s.id=c.id_statut
            join utilisateur u on u.id=c.id_candidat
            join offre_emploi o on o.id=c.id_offre join poste p on p.id=o.id_poste
            where s.code='EN_COURS' and e.date_entretien >= ? and e.date_entretien < ?
            order by e.date_entretien, e.id limit 5
            """, (rs,index) -> Map.<String,Object>of(
                "titre", "Entretien · " + rs.getString("intitule"),
                "description", rs.getString("email"),
                "dateTri", rs.getTimestamp("date_entretien").toLocalDateTime(),
                "date", rs.getTimestamp("date_entretien").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM à HH:mm")),
                "lien", "/rh/offre/" + rs.getInt("id") + "/candidats"), maintenant, maintenant.plusDays(7)));
        echeances.addAll(jdbc.query("""
            select d.type, d.date_expiration, u.email, u.id
            from document_administratif d join utilisateur u on u.id=d.id_utilisateur
            where d.date_expiration between ? and ?
            order by d.date_expiration, d.id limit 5
            """, (rs,index) -> Map.<String,Object>of(
                "titre", "Document à renouveler · " + rs.getString("type"),
                "description", rs.getString("email"),
                "dateTri", rs.getDate("date_expiration").toLocalDate().atStartOfDay(),
                "date", rs.getDate("date_expiration").toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                "lien", "/rh/dossiers/" + rs.getInt("id") + "#documents"), aujourdHui, aujourdHui.plusDays(30)));
        return echeances.stream().sorted(Comparator.comparing(e -> (LocalDateTime)e.get("dateTri"))).limit(5).toList();
    }
}
