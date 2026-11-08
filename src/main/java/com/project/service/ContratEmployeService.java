package com.project.service;

import com.project.entity.Contrat;
import com.project.entity.Employe;
import com.project.entity.TypeContrat;
import com.project.repository.ContratRepository;
import com.project.repository.EmployeRepository;
import com.project.repository.TypeContratRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion RH des contrats rattaches aux employes reels.
 *
 * <p>Le contrat initial reste lie a sa candidature. Les contrats suivants
 * (passage CE -> CDI, renouvellement, nouveau CE...) sont rattaches a
 * l'employe et conserves dans l'historique. Le contrat courant de l'employe
 * est celui pointe par {@code employe.id_contrat_actuel}.</p>
 */
@Service
public class ContratEmployeService {
    private final ContratRepository contratRepo;
    private final EmployeRepository employeRepo;
    private final TypeContratRepository typeContratRepo;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ContratEmployeService(ContratRepository contratRepo, EmployeRepository employeRepo,
            TypeContratRepository typeContratRepo, JdbcTemplate jdbc, Clock clock) {
        this.contratRepo = contratRepo;
        this.employeRepo = employeRepo;
        this.typeContratRepo = typeContratRepo;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> employesAvecContrat() {
        return jdbc.queryForList("""
                select e.id_employe as id, e.date_embauche as dateEmbauche,
                       u.email as email,
                       c.id as contratId, c.date_debut as dateDebut, c.date_fin as dateFin,
                       c.duree_mois as dureeMois, c.statut as statut,
                       tc.code as typeCode, tc.libelle as typeLibelle
                from employe e
                join utilisateur u on u.id = e.id_utilisateur
                join contrat c on c.id = e.id_contrat_actuel
                join type_contrat tc on tc.id = c.id_type_contrat
                order by e.id_employe
                """);
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<Map<String, Object>>> historiquesParEmploye(List<Map<String, Object>> employes) {
        Map<Integer, List<Map<String, Object>>> historiques = new HashMap<>();
        for (Map<String, Object> employe : employes) {
            Integer id = ((Number) employe.get("id")).intValue();
            historiques.put(id, historique(id));
        }
        return historiques;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> historique(Integer idEmp) {
        return jdbc.queryForList("""
                select c.id, c.date_debut as dateDebut, c.date_fin as dateFin,
                       c.duree_mois as dureeMois, c.statut,
                       tc.code as typeCode, tc.libelle as typeLibelle
                from contrat c
                join type_contrat tc on tc.id = c.id_type_contrat
                where c.id_employe = ?
                order by c.date_debut desc nulls last, c.id desc
                """, idEmp);
    }

    @Transactional(readOnly = true)
    public List<TypeContrat> typesContrat() {
        return typeContratRepo.findAll(Sort.by("id"));
    }

    /**
     * Cree le contrat suivant de l'employe. Si la date de commencement est
     * aujourd'hui ou passee, il devient immediatement le contrat courant.
     * Sinon il reste PROGRAMME jusqu'a l'activation automatique a sa date.
     */
    @Transactional
    public Contrat programmer(Integer idEmp, String codeType, LocalDate dateDebut, Integer dureeMois,
            Integer rhId) {
        if (idEmp == null) {
            throw new IllegalArgumentException("Selectionnez un employe.");
        }
        if (codeType == null || codeType.isBlank()) {
            throw new IllegalArgumentException("Choisissez un type de contrat.");
        }
        Employe employe = employeRepo.findByIdForUpdate(idEmp)
                .orElseThrow(() -> new IllegalArgumentException("Employe introuvable."));
        Contrat actuel = contratRepo.findById(employe.getContratId())
                .orElseThrow(() -> new IllegalStateException("Contrat actuel de l'employe introuvable."));
        TypeContrat type = typeContratRepo.findByCode(codeType.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Type de contrat invalide."));

        LocalDate aujourdhui = LocalDate.now(clock);
        if (dateDebut == null) {
            throw new IllegalArgumentException("La date de commencement est obligatoire.");
        }
        LocalDate commencement = dateDebut;
        if (dureeMois != null && dureeMois <= 0) {
            throw new IllegalArgumentException("La duree doit etre superieure a zero.");
        }
        boolean cdi = "CDI".equals(type.getCode());
        if (!cdi && dureeMois == null) {
            throw new IllegalArgumentException("La duree en mois est obligatoire pour un contrat d'essai.");
        }

        Contrat contrat = new Contrat();
        contrat.setIdEmploye(employe.getId());
        // Le contrat successif reprend les conditions de travail du contrat courant.
        contrat.setCondition(actuel.getCondition());
        contrat.setTypeContrat(type);
        contrat.setDateDebut(commencement);
        contrat.setDureeMois(cdi ? null : dureeMois);
        contrat.setDateFin(cdi ? null : commencement.plusMonths(dureeMois));
        contrat.setIdRhPreparation(rhId);

        boolean immediat = !commencement.isAfter(aujourdhui);
        contrat.setStatut(immediat ? Contrat.STATUT_FINALISE : Contrat.STATUT_PROGRAMME);
        contrat = contratRepo.save(contrat);

        if (immediat) {
            // Modification immediate du contrat de l'employe : le nouveau
            // contrat devient courant, l'ancien reste dans l'historique.
            employe.setContratId(contrat.getId());
            employeRepo.save(employe);
        }
        return contrat;
    }

    /**
     * Active les contrats programmes dont la date de commencement est atteinte.
     * Les traite par date croissante pour que le dernier contrat programme
     * devienne bien le contrat courant.
     */
    @Transactional
    public int activerContratsDus() {
        List<Contrat> contrats = contratRepo
                .findByStatutAndDateDebutLessThanEqualOrderByDateDebutAscIdAsc(
                        Contrat.STATUT_PROGRAMME, LocalDate.now(clock));
        for (Contrat contrat : contrats) {
            Employe employe = employeRepo.findByIdForUpdate(contrat.getIdEmploye())
                    .orElse(null);
            if (employe == null) {
                continue;
            }
            contrat.setStatut(Contrat.STATUT_FINALISE);
            contratRepo.save(contrat);
            employe.setContratId(contrat.getId());
            employeRepo.save(employe);
        }
        return contrats.size();
    }
}
