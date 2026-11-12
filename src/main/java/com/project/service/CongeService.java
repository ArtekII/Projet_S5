package com.project.service;

import com.project.entity.Conge;
import com.project.entity.Employe;
import com.project.entity.HistoriqueConge;
import com.project.entity.SoldeConge;
import com.project.entity.TypeConge;
import com.project.entity.Utilisateur;
import com.project.repository.CongeRepository;
import com.project.repository.EmployeRepository;
import com.project.repository.HistoriqueCongeRepository;
import com.project.repository.SoldeCongeRepository;
import com.project.repository.TypeCongeRepository;
import com.project.repository.UtilisateurRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestion des conges des employes.
 *
 * <p>Le solde est calcule par periode annuelle ancree sur la date d'embauche.
 * Le mois en cours est compte des son premier jour : un employe embauche
 * aujourd'hui acquiert deja une mensualite. Les demandes en attente reservent
 * le solde sans le consommer ; le solde n'est decremente qu'a la validation RH.</p>
 */
@Service
public class CongeService {
    private static final Logger log = LoggerFactory.getLogger(CongeService.class);
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final int MOIS_PAR_PERIODE = 12;

    private final EmployeRepository employes;
    private final UtilisateurRepository utilisateurs;
    private final TypeCongeRepository types;
    private final CongeRepository conges;
    private final SoldeCongeRepository soldes;
    private final HistoriqueCongeRepository historiques;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CongeService(EmployeRepository employes, UtilisateurRepository utilisateurs,
            TypeCongeRepository types, CongeRepository conges, SoldeCongeRepository soldes,
            HistoriqueCongeRepository historiques, JdbcTemplate jdbc, Clock clock) {
        this.employes = employes;
        this.utilisateurs = utilisateurs;
        this.types = types;
        this.conges = conges;
        this.soldes = soldes;
        this.historiques = historiques;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // Espace employe
    // ------------------------------------------------------------------

    @Transactional
    public Map<String, Object> espaceEmploye(Integer utilisateurId) {
        Utilisateur utilisateur = utilisateur(utilisateurId);
        Employe employe = employes.findByUtilisateurId(utilisateur.getId()).orElse(null);
        Map<String, Object> vue = new HashMap<>();
        vue.put("employeConge", employe);
        if (employe == null) {
            return vue;
        }
        actualiserSoldes(employe);
        vue.put("typesConge", types.findByActifTrueOrderByIdAsc());
        vue.put("soldesConges", soldesEmploye(employe.getId()));
        vue.put("demandesConges", demandesEmploye(employe.getId()));
        vue.put("aujourdhuiConge", LocalDate.now(clock));
        return vue;
    }

    @Transactional
    public void demander(Integer utilisateurId, Integer typeCongeId, LocalDate dateDebut,
            LocalDate dateFin, String commentaire) {
        Utilisateur demandeur = utilisateur(utilisateurId);
        Employe employe = employes.findByUtilisateurId(demandeur.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Votre compte n'est pas encore rattaché à un employé."));
        TypeConge type = types.findById(typeCongeId)
                .filter(TypeConge::isActif)
                .orElseThrow(() -> new IllegalArgumentException("Type de congé invalide."));

        LocalDate aujourdhui = LocalDate.now(clock);
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("Renseignez les dates de début et de fin.");
        }
        if (dateDebut.isBefore(aujourdhui)) {
            throw new IllegalArgumentException("La date de début ne peut pas être passée.");
        }
        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit suivre la date de début.");
        }
        if (ChronoUnit.DAYS.between(dateDebut, dateFin) > 366) {
            throw new IllegalArgumentException("Une demande de congé ne peut pas dépasser un an.");
        }
        BigDecimal nbJours = joursOuvres(dateDebut, dateFin);
        if (nbJours.signum() <= 0) {
            throw new IllegalArgumentException("La demande doit contenir au moins un jour ouvré (lundi au vendredi).");
        }
        String motif = nettoyer(commentaire);
        if (motif != null && motif.length() > 500) {
            throw new IllegalArgumentException("Le commentaire est limité à 500 caractères.");
        }

        // Serialise les demandes d'un meme employe avant de verifier les chevauchements et le solde.
        employe = employes.findByIdForUpdate(employe.getId())
                .orElseThrow(() -> new IllegalStateException("Employé introuvable."));

        Integer chevauchements = jdbc.queryForObject("""
                select count(*) from conge
                where id_employe = ?
                  and statut in ('EN_ATTENTE', 'VALIDE')
                  and date_debut <= ? and date_fin >= ?
                """, Integer.class, employe.getId(), dateFin, dateDebut);
        if (chevauchements != null && chevauchements > 0) {
            throw new IllegalArgumentException("Une autre demande de congé couvre déjà cette période.");
        }

        BigDecimal soldeAvant = null;
        if (type.isDecompteSolde() && type.getAcquisitionMensuelle() != null) {
            Periode periode = periodePour(employe.getDateEmbauche(), dateDebut);
            if (periode == null) {
                throw new IllegalArgumentException("La date demandée est antérieure à l'embauche.");
            }
            if (dateFin.isAfter(periode.fin())) {
                throw new IllegalArgumentException("La demande doit se terminer dans la période de congés en cours ("
                        + periode.debut() + " au " + periode.fin() + ").");
            }
            SoldeConge solde = actualiserSolde(employe, type, periode);
            BigDecimal disponible = disponible(solde);
            BigDecimal reserve = jdbc.queryForObject("""
                    select coalesce(sum(nb_jours), 0) from conge
                    where id_employe = ? and id_type_conge = ? and statut = 'EN_ATTENTE'
                      and date_debut <= ? and date_fin >= ?
                    """, BigDecimal.class, employe.getId(), type.getId(), periode.fin(), periode.debut());
            BigDecimal reste = disponible.subtract(reserve == null ? ZERO : reserve);
            soldeAvant = reste;
            if (nbJours.compareTo(reste) > 0) {
                throw new IllegalArgumentException("Solde insuffisant pour " + type.getLibelle()
                        + " : " + afficher(reste) + " jour(s) disponible(s), " + afficher(nbJours)
                        + " demandé(s).");
            }
        }

        Conge conge = new Conge();
        conge.setEmploye(employe);
        conge.setTypeConge(type);
        conge.setDateDebut(dateDebut);
        conge.setDateFin(dateFin);
        conge.setNbJours(nbJours);
        conge.setStatut(Conge.STATUT_EN_ATTENTE);
        conge.setCommentaire(motif);
        conge.setDateDemande(LocalDateTime.now(clock));
        conge = conges.saveAndFlush(conge);

        tracer(conge, employe, demandeur, HistoriqueConge.ACTION_CREATION, null,
                Conge.STATUT_EN_ATTENTE, nbJours, soldeAvant, soldeAvant, null);
    }

    @Transactional
    public void annuler(Integer utilisateurId, Integer congeId) {
        Utilisateur auteur = utilisateur(utilisateurId);
        Conge conge = conges.findByIdForUpdate(congeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de congé introuvable."));
        if (!conge.getEmploye().getUtilisateurId().equals(auteur.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!Conge.STATUT_EN_ATTENTE.equals(conge.getStatut())) {
            throw new IllegalArgumentException("Seule une demande en attente peut être annulée.");
        }
        conge.setStatut(Conge.STATUT_ANNULE);
        conge.setDateDecision(LocalDateTime.now(clock));
        conges.save(conge);
        tracer(conge, conge.getEmploye(), auteur, HistoriqueConge.ACTION_ANNULATION,
                Conge.STATUT_EN_ATTENTE, Conge.STATUT_ANNULE, conge.getNbJours(), null, null, null);
    }

    // ------------------------------------------------------------------
    // Espace RH
    // ------------------------------------------------------------------

    @Transactional
    public Map<String, Object> espaceRh(Integer rhId) {
        utilisateurRh(rhId);
        actualiserTousLesSoldes();
        Map<String, Object> vue = new HashMap<>();
        vue.put("demandes", jdbc.queryForList("""
                select c.id_conge as id, e.id_employe as employeId, u.email as email,
                       tc.code as typeCode, tc.libelle as typeLibelle,
                       c.date_debut as dateDebut, c.date_fin as dateFin, c.nb_jours as nbJours,
                       c.statut, c.commentaire, c.date_demande as dateDemande,
                       c.date_decision as dateDecision,
                       (s.jours_acquis - s.jours_pris) as soldeDisponible
                from conge c
                join employe e on e.id_employe = c.id_employe
                join utilisateur u on u.id = e.id_utilisateur
                join type_conge tc on tc.id_type_conge = c.id_type_conge
                left join solde_conge s on s.id_employe = c.id_employe
                    and s.id_type_conge = c.id_type_conge
                    and c.date_debut between s.periode_debut and s.periode_fin
                order by case when c.statut = 'EN_ATTENTE' then 0 else 1 end,
                         c.date_demande desc, c.id_conge desc
                """));
        vue.put("soldes", jdbc.queryForList("""
                select s.id_solde as id, e.id_employe as employeId, u.email as email,
                       tc.code as typeCode, tc.libelle as typeLibelle,
                       s.periode_debut as periodeDebut, s.periode_fin as periodeFin,
                       s.jours_acquis as joursAcquis, s.jours_pris as joursPris,
                       (s.jours_acquis - s.jours_pris) as soldeDisponible,
                       coalesce((select sum(c2.nb_jours) from conge c2
                           where c2.id_employe = s.id_employe
                             and c2.id_type_conge = s.id_type_conge
                             and c2.statut = 'EN_ATTENTE'
                             and c2.date_debut between s.periode_debut and s.periode_fin), 0) as joursEnAttente,
                       (s.jours_acquis - s.jours_pris - coalesce((select sum(c2.nb_jours) from conge c2
                           where c2.id_employe = s.id_employe
                             and c2.id_type_conge = s.id_type_conge
                             and c2.statut = 'EN_ATTENTE'
                             and c2.date_debut between s.periode_debut and s.periode_fin), 0)) as soldeReserve
                from solde_conge s
                join employe e on e.id_employe = s.id_employe
                join utilisateur u on u.id = e.id_utilisateur
                join type_conge tc on tc.id_type_conge = s.id_type_conge
                order by u.email, tc.id_type_conge, s.periode_debut desc
                """));
        return vue;
    }

    @Transactional
    public void valider(Integer rhId, Integer congeId, String commentaire) {
        Utilisateur rh = utilisateurRh(rhId);
        Conge conge = conges.findByIdForUpdate(congeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de congé introuvable."));
        if (!Conge.STATUT_EN_ATTENTE.equals(conge.getStatut())) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée.");
        }
        String motif = nettoyer(commentaire);
        if (motif != null && motif.length() > 500) {
            throw new IllegalArgumentException("Le commentaire est limité à 500 caractères.");
        }

        Employe employe = employes.findByIdForUpdate(conge.getEmploye().getId())
                .orElseThrow(() -> new IllegalStateException("Employé introuvable."));
        TypeConge type = conge.getTypeConge();
        BigDecimal soldeAvant = null;
        BigDecimal soldeApres = null;

        if (type.isDecompteSolde() && type.getAcquisitionMensuelle() != null) {
            Periode periode = periodePour(employe.getDateEmbauche(), conge.getDateDebut());
            if (periode == null || conge.getDateFin().isAfter(periode.fin())) {
                throw new IllegalArgumentException("La demande sort de la période de congés de l'employé.");
            }
            SoldeConge solde = actualiserSolde(employe, type, periode);
            soldeAvant = disponible(solde);
            BigDecimal autresEnAttente = jdbc.queryForObject("""
                    select coalesce(sum(nb_jours), 0) from conge
                    where id_employe = ? and id_type_conge = ? and statut = 'EN_ATTENTE'
                      and id_conge <> ? and date_debut <= ? and date_fin >= ?
                    """, BigDecimal.class, employe.getId(), type.getId(), conge.getId(),
                    conge.getDateFin(), conge.getDateDebut());
            BigDecimal reste = soldeAvant.subtract(autresEnAttente == null ? ZERO : autresEnAttente);
            if (conge.getNbJours().compareTo(reste) > 0) {
                throw new IllegalArgumentException("Solde insuffisant pour valider cette demande : "
                        + afficher(reste) + " jour(s) disponible(s), " + afficher(conge.getNbJours())
                        + " demandé(s).");
            }
            solde.setJoursPris(solde.getJoursPris().add(conge.getNbJours()));
            solde.setDateMaj(LocalDateTime.now(clock));
            soldes.save(solde);
            soldeApres = disponible(solde);
        }

        conge.setStatut(Conge.STATUT_VALIDE);
        conge.setDateDecision(LocalDateTime.now(clock));
        conges.save(conge);
        tracer(conge, employe, rh, HistoriqueConge.ACTION_VALIDATION, Conge.STATUT_EN_ATTENTE,
                Conge.STATUT_VALIDE, conge.getNbJours(), soldeAvant, soldeApres, motif);
    }

    @Transactional
    public void refuser(Integer rhId, Integer congeId, String commentaire) {
        Utilisateur rh = utilisateurRh(rhId);
        Conge conge = conges.findByIdForUpdate(congeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de congé introuvable."));
        if (!Conge.STATUT_EN_ATTENTE.equals(conge.getStatut())) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée.");
        }
        String motif = nettoyer(commentaire);
        if (motif != null && motif.length() > 500) {
            throw new IllegalArgumentException("Le commentaire est limité à 500 caractères.");
        }
        conge.setStatut(Conge.STATUT_REFUSE);
        conge.setDateDecision(LocalDateTime.now(clock));
        conges.save(conge);
        tracer(conge, conge.getEmploye(), rh, HistoriqueConge.ACTION_REFUS, Conge.STATUT_EN_ATTENTE,
                Conge.STATUT_REFUSE, conge.getNbJours(), null, null, motif);
    }

    // ------------------------------------------------------------------
    // Soldes et acquisition
    // ------------------------------------------------------------------

    /** Recalcule les soldes de la periode en cours pour tous les employes. */
    @Transactional
    public int actualiserTousLesSoldes() {
        int total = 0;
        for (Employe employe : employes.findAll(Sort.by("id"))) {
            try {
                total += actualiserSoldes(employe);
            } catch (RuntimeException e) {
                log.warn("Solde de congés non actualisé pour l'employé {} : {}", employe.getId(), e.getMessage());
            }
        }
        return total;
    }

    private int actualiserSoldes(Employe employe) {
        Employe verrouille = employes.findByIdForUpdate(employe.getId()).orElse(null);
        if (verrouille == null) {
            return 0;
        }
        LocalDate aujourdhui = LocalDate.now(clock);
        Periode periode = periodePour(verrouille.getDateEmbauche(), aujourdhui);
        if (periode == null) {
            return 0;
        }
        int total = 0;
        for (TypeConge type : types.findByActifTrueOrderByIdAsc()) {
            if (!type.isDecompteSolde() || type.getAcquisitionMensuelle() == null) {
                continue;
            }
            actualiserSolde(verrouille, type, periode);
            total++;
        }
        return total;
    }

    private SoldeConge actualiserSolde(Employe employe, TypeConge type, Periode periode) {
        SoldeConge solde = soldes.findForUpdate(employe.getId(), type.getId(), periode.debut())
                .orElseGet(() -> {
                    SoldeConge nouveau = new SoldeConge();
                    nouveau.setEmploye(employe);
                    nouveau.setTypeConge(type);
                    nouveau.setPeriodeDebut(periode.debut());
                    nouveau.setJoursAcquis(ZERO);
                    nouveau.setJoursPris(ZERO);
                    return nouveau;
                });
        solde.setPeriodeFin(periode.fin());
        solde.setJoursAcquis(joursAcquis(type, periode, LocalDate.now(clock)));
        solde.setDateMaj(LocalDateTime.now(clock));
        return soldes.save(solde);
    }

    private List<Map<String, Object>> soldesEmploye(Integer employeId) {
        return jdbc.queryForList("""
                select s.id_solde as id, tc.code as typeCode, tc.libelle as typeLibelle,
                       tc.decompte_solde as decompteSolde,
                       s.periode_debut as periodeDebut, s.periode_fin as periodeFin,
                       s.jours_acquis as joursAcquis, s.jours_pris as joursPris,
                       (s.jours_acquis - s.jours_pris) as soldeDisponible,
                       coalesce((select sum(c2.nb_jours) from conge c2
                           where c2.id_employe = s.id_employe
                             and c2.id_type_conge = s.id_type_conge
                             and c2.statut = 'EN_ATTENTE'
                             and c2.date_debut between s.periode_debut and s.periode_fin), 0) as joursEnAttente,
                       (s.jours_acquis - s.jours_pris - coalesce((select sum(c2.nb_jours) from conge c2
                           where c2.id_employe = s.id_employe
                             and c2.id_type_conge = s.id_type_conge
                             and c2.statut = 'EN_ATTENTE'
                             and c2.date_debut between s.periode_debut and s.periode_fin), 0)) as soldeReserve
                from solde_conge s
                join type_conge tc on tc.id_type_conge = s.id_type_conge
                where s.id_employe = ?
                order by tc.id_type_conge, s.periode_debut desc
                """, employeId);
    }

    private List<Map<String, Object>> demandesEmploye(Integer employeId) {
        return jdbc.queryForList("""
                select c.id_conge as id, tc.code as typeCode, tc.libelle as typeLibelle,
                       c.date_debut as dateDebut, c.date_fin as dateFin, c.nb_jours as nbJours,
                       c.statut, c.commentaire, c.date_demande as dateDemande,
                       c.date_decision as dateDecision
                from conge c
                join type_conge tc on tc.id_type_conge = c.id_type_conge
                where c.id_employe = ?
                order by c.date_debut desc, c.id_conge desc
                """, employeId);
    }

    /**
     * Jours ouvrés (lundi-vendredi) compris entre les deux dates incluses.
     * Les jours fériés ne sont pas déduits : aucun référentiel n'est encore
     * disponible dans l'application.
     */
    static BigDecimal joursOuvres(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return ZERO;
        }
        long jours = 0;
        for (LocalDate date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            DayOfWeek jour = date.getDayOfWeek();
            if (jour != DayOfWeek.SATURDAY && jour != DayOfWeek.SUNDAY) {
                jours++;
            }
        }
        return BigDecimal.valueOf(jours).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Periode annuelle de 12 mois contenant la date, ancree sur la date d'embauche. */
    static Periode periodePour(LocalDate embauche, LocalDate date) {
        if (embauche == null || date == null || date.isBefore(embauche)) {
            return null;
        }
        long annees = ChronoUnit.YEARS.between(embauche, date);
        LocalDate debut = embauche.plusYears(annees);
        if (debut.isAfter(date)) {
            annees--;
            debut = embauche.plusYears(annees);
        }
        return new Periode(debut, debut.plusYears(1).minusDays(1));
    }

    /**
     * Le mois en cours compte des son premier jour. Le nombre de mensualites
     * est plafonne a 12 par periode.
     */
    static BigDecimal joursAcquis(TypeConge type, Periode periode, LocalDate reference) {
        if (type == null || periode == null || type.getAcquisitionMensuelle() == null
                || !type.isDecompteSolde() || reference == null || reference.isBefore(periode.debut())) {
            return ZERO;
        }
        LocalDate fin = reference.isAfter(periode.fin()) ? periode.fin() : reference;
        long mois = ChronoUnit.MONTHS.between(periode.debut(), fin) + 1;
        if (mois > MOIS_PAR_PERIODE) {
            mois = MOIS_PAR_PERIODE;
        }
        if (mois <= 0) {
            return ZERO;
        }
        return type.getAcquisitionMensuelle()
                .multiply(BigDecimal.valueOf(mois))
                .setScale(2, RoundingMode.HALF_UP);
    }

    record Periode(LocalDate debut, LocalDate fin) {
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private Utilisateur utilisateur(Integer id) {
        Utilisateur utilisateur = utilisateurs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return utilisateur;
    }

    private Utilisateur utilisateurRh(Integer id) {
        Utilisateur utilisateur = utilisateur(id);
        if (utilisateur.getRole() == null || !"RH".equals(utilisateur.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return utilisateur;
    }

    private BigDecimal disponible(SoldeConge solde) {
        BigDecimal acquis = solde.getJoursAcquis() == null ? ZERO : solde.getJoursAcquis();
        BigDecimal pris = solde.getJoursPris() == null ? ZERO : solde.getJoursPris();
        return acquis.subtract(pris);
    }

    private String afficher(BigDecimal valeur) {
        return (valeur == null ? ZERO : valeur).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nettoyer(String valeur) {
        if (valeur == null) {
            return null;
        }
        String propre = valeur.trim();
        return propre.isEmpty() ? null : propre;
    }

    private void tracer(Conge conge, Employe employe, Utilisateur auteur, String action,
            String ancienStatut, String nouveauStatut, BigDecimal nbJours,
            BigDecimal soldeAvant, BigDecimal soldeApres, String commentaire) {
        HistoriqueConge historique = new HistoriqueConge();
        historique.setConge(conge);
        historique.setEmploye(employe);
        historique.setAuteur(auteur);
        historique.setAction(action);
        historique.setAncienStatut(ancienStatut);
        historique.setNouveauStatut(nouveauStatut);
        historique.setNbJours(nbJours);
        historique.setSoldeAvant(soldeAvant);
        historique.setSoldeApres(soldeApres);
        historique.setDateAction(LocalDateTime.now(clock));
        historique.setCommentaire(commentaire);
        historiques.save(historique);
    }
}
