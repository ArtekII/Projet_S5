package com.project.service;

import com.project.dto.*;
import com.project.entity.*;
import com.project.repository.*;
import jakarta.validation.Validator;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PointageService {
    private final EmployeRepository employes;
    private final UtilisateurRepository utilisateurs;
    private final PosteRepository postes;
    private final PlanningRepository plannings;
    private final PointageRepository pointages;
    private final AlertePointageRepository alertes;
    private final CorrectionPointageRepository corrections;
    private final VariablePaiePointageRepository variables;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Validator validator;

    public PointageService(EmployeRepository employes, UtilisateurRepository utilisateurs, PosteRepository postes,
            PlanningRepository plannings, PointageRepository pointages, AlertePointageRepository alertes,
            CorrectionPointageRepository corrections, VariablePaiePointageRepository variables,
            JdbcTemplate jdbc, Clock clock, Validator validator) {
        this.employes = employes; this.utilisateurs = utilisateurs; this.postes = postes; this.plannings = plannings;
        this.pointages = pointages; this.alertes = alertes; this.corrections = corrections; this.variables = variables;
        this.jdbc = jdbc; this.clock = clock; this.validator = validator;
    }

    private Utilisateur autoriser(Integer id, String... roles) {
        Utilisateur utilisateur = utilisateurs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!Boolean.TRUE.equals(utilisateur.getActif()) || (roles.length > 0 && (utilisateur.getRole() == null
                || !Arrays.asList(roles).contains(utilisateur.getRole().getCode())))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return utilisateur;
    }

    public LocalDate aujourdhui() { return LocalDate.now(clock); }
    private LocalDateTime maintenant() { return LocalDateTime.now(clock).withNano(0); }

    public List<Poste> postesRh(Integer acteur) {
        autoriser(acteur, "RH"); return postes.findAll(Sort.by("intitule"));
    }

    public List<Planning> historiquePlannings(Integer acteur) {
        autoriser(acteur, "RH"); return plannings.findAllByOrderByPoste_IdAscJourSemaineAscDateEffetDesc();
    }

    public Planning definirPlanning(Integer acteur, PlanningForm saisie) {
        Utilisateur rh = autoriser(acteur, "RH");
        if (!validator.validate(saisie).isEmpty()) throw new IllegalArgumentException("Vérifiez le poste, le jour et la date d'effet.");
        // La ligne poste serialise les creations concurrentes d'une meme version.
        var existants = jdbc.queryForList("select id from poste where id = ? for update", Integer.class, saisie.getPosteId());
        if (existants.isEmpty()) throw new IllegalArgumentException("Poste introuvable.");
        if (plannings.existsByPoste_IdAndJourSemaineAndDateEffet(saisie.getPosteId(), saisie.getJourSemaine(), saisie.getDateEffet())) {
            throw new IllegalArgumentException("Un planning existe déjà pour ce poste, ce jour et cette date d'effet. Choisissez une nouvelle date d'effet.");
        }
        Planning planning = new Planning();
        planning.setPoste(postes.findById(saisie.getPosteId()).orElseThrow());
        planning.setJourSemaine(saisie.getJourSemaine()); planning.setDateEffet(saisie.getDateEffet());
        planning.setJourTravaille(saisie.isJourTravaille());
        if (saisie.isJourTravaille()) {
            if (saisie.getHeureDebut() == null || saisie.getHeureFin() == null || saisie.getHeureDebut().equals(saisie.getHeureFin())) {
                throw new IllegalArgumentException("Renseignez des heures de début et de fin différentes.");
            }
            if ((saisie.getPauseDebut() == null) != (saisie.getPauseFin() == null)) {
                throw new IllegalArgumentException("Renseignez le début et la fin de pause, ou laissez les deux vides.");
            }
            planning.setHeureDebut(saisie.getHeureDebut()); planning.setHeureFin(saisie.getHeureFin());
            planning.setPauseDebut(saisie.getPauseDebut()); planning.setPauseFin(saisie.getPauseFin());
            var plage = CalculPointage.creneau(saisie.getDateEffet(), planning);
            if (plage.pauseDebut() != null && (plage.pauseDebut().isBefore(plage.debut()) || plage.pauseFin().isAfter(plage.fin())
                    || !plage.pauseFin().isAfter(plage.pauseDebut())
                    || Duration.between(plage.pauseDebut(), plage.pauseFin()).compareTo(Duration.between(plage.debut(), plage.fin())) >= 0)) {
                throw new IllegalArgumentException("La pause doit être comprise dans l'horaire et laisser un temps de travail positif.");
            }
        }
        planning.setRh(rh); planning.setCreeLe(maintenant()); return plannings.saveAndFlush(planning);
    }

    private record Affectation(Integer posteId, boolean active) { }

    private Affectation affectation(Employe employe, LocalDate date) {
        var lignes = jdbc.queryForList("""
                select coalesce(ct.id_poste, o.id_poste, ocv.id_poste) as poste
                from employe e join contrat c on c.id_employe = e.id_employe or c.id = e.id_contrat_actuel
                join condition_travail ct on ct.id = c.id_condition
                left join candidature ca on ca.id = ct.id_candidature
                left join offre_emploi o on o.id = ca.id_offre
                left join cv_candidat cv on cv.id = e.id_cv_candidat
                left join offre_emploi ocv on ocv.id = cv.id_offre
                where e.id_employe = ? and e.date_embauche <= ? and c.statut = 'FINALISE'
                  and coalesce(c.date_debut, e.date_embauche) <= ? and (c.date_fin is null or c.date_fin >= ?)
                order by coalesce(c.date_debut, e.date_embauche) desc, c.id desc limit 1
                """, employe.getId(), date, date, date);
        if (lignes.isEmpty()) return new Affectation(null, false);
        Number poste = (Number) lignes.getFirst().get("poste");
        return new Affectation(poste == null ? null : poste.intValue(), true);
    }

    private Planning planningPour(Employe employe, LocalDate date) {
        Integer poste = affectation(employe, date).posteId();
        return poste == null ? null : plannings.findFirstByPoste_IdAndJourSemaineAndDateEffetLessThanEqualOrderByDateEffetDesc(
                poste, date.getDayOfWeek().getValue(), date).orElse(null);
    }

    public void connecter(Integer utilisateurId, String jeton) {
        autoriser(utilisateurId);
        Optional<Employe> dossier = employes.findByUtilisateurId(utilisateurId);
        if (dossier.isEmpty()) return; // Les comptes candidats sans embauche ne pointent pas.
        Employe employe = employes.findByIdForUpdate(dossier.get().getId()).orElseThrow();
        if (pointages.findByJeton(jeton).isPresent()) return;
        LocalDateTime entree = maintenant();
        LocalDate date = entree.toLocalDate();
        LocalDate dateVeille = date.minusDays(1);
        Planning veille = pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employe.getId(), dateVeille)
                .stream().map(Pointage::getPlanning).filter(Objects::nonNull).findFirst().orElse(null);
        if (veille == null) veille = planningPour(employe, dateVeille);
        var horaireVeille = CalculPointage.creneau(date.minusDays(1), veille);
        if (horaireVeille != null && horaireVeille.fin().toLocalDate().equals(date) && entree.isBefore(horaireVeille.fin())) date = date.minusDays(1);
        if (!affectation(employe, date).active()) return;
        List<Pointage> existants = pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employe.getId(), date);
        Planning planning = existants.stream().map(Pointage::getPlanning).filter(Objects::nonNull).findFirst().orElse(null);
        if (planning == null) planning = planningPour(employe, date);
        Pointage pointage = new Pointage();
        pointage.setEmploye(employe); pointage.setPlanning(planning); pointage.setDate(date);
        Integer posteId = affectation(employe, date).posteId();
        pointage.setPoste(planning != null ? planning.getPoste() : posteId == null ? null : postes.findById(posteId).orElse(null));
        pointage.setConnexion(entree); pointage.setJeton(jeton); pointages.saveAndFlush(pointage);
        bilan(employe, date);
    }

    public void deconnecter(String jeton) {
        if (jeton == null) return;
        Optional<Integer> idEmploye = pointages.employePourJeton(jeton);
        if (idEmploye.isEmpty()) return;
        Employe employe = employes.findByIdForUpdate(idEmploye.get()).orElseThrow();
        Pointage pointage = pointages.findByJeton(jeton).orElseThrow();
        if (pointage.getDeconnexion() == null) { pointage.setDeconnexion(maintenant()); pointages.saveAndFlush(pointage); }
        bilan(employe, pointage.getDate());
    }

    public void expirerSession(String jeton) {
        if (jeton == null) return;
        Optional<Integer> idEmploye = pointages.employePourJeton(jeton);
        if (idEmploye.isEmpty()) return;
        Employe employe = employes.findByIdForUpdate(idEmploye.get()).orElseThrow();
        Pointage pointage = pointages.findByJeton(jeton).orElseThrow();
        if (pointage.getSortieEffective() == null) {
            pointage.setSessionExpiree(true); pointages.saveAndFlush(pointage); bilan(employe, pointage.getDate());
        }
    }

    private BilanPointage bilan(Employe employe, LocalDate date) {
        List<Pointage> sessions = pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employe.getId(), date);
        VariablePaiePointage transmission = variables.findByEmploye_IdAndDate(employe.getId(), date).orElse(null);
        Planning planning = transmission == null ? sessions.stream().map(Pointage::getPlanning).filter(Objects::nonNull).findFirst().orElse(null) : transmission.getPlanning();
        if (planning == null) planning = planningPour(employe, date);
        var calcul = CalculPointage.calculer(date, planning, sessions, maintenant());
        synchroniserAlertes(employe, date, calcul.anomalies());
        var creneau = CalculPointage.creneau(date, planning);
        boolean transmissible = transmission == null && date.isBefore(aujourdhui()) && planning != null
                && !calcul.enCours() && (creneau == null || !maintenant().isBefore(creneau.fin()))
                && calcul.anomalies().stream().noneMatch(AnomaliePointage::isBloquante);
        String email = utilisateurs.findById(employe.getUtilisateurId()).orElseThrow().getEmail();
        return new BilanPointage(employe, email, date, planning, sessions, calcul, transmission, transmissible,
                corrections.findByPointage_Employe_IdAndPointage_DateOrderByIdDesc(employe.getId(), date));
    }

    private void synchroniserAlertes(Employe employe, LocalDate date, Set<AnomaliePointage> anomalies) {
        Map<String, AlertePointage> existantes = new HashMap<>();
        for (AlertePointage alerte : alertes.findByEmploye_IdAndDate(employe.getId(), date)) existantes.put(alerte.getCode(), alerte);
        for (AnomaliePointage anomalie : anomalies) {
            AlertePointage alerte = existantes.remove(anomalie.name());
            if (alerte == null) {
                alerte = new AlertePointage(); alerte.setEmploye(employe); alerte.setDate(date); alerte.setCode(anomalie.name());
                alerte.setMessage(anomalie.getMessage()); alerte.setDetecteeLe(maintenant());
            }
            alerte.setResolueLe(null); alertes.save(alerte);
        }
        for (AlertePointage alerte : existantes.values()) if (alerte.getResolueLe() == null) alerte.setResolueLe(maintenant());
    }

    public Map<String, Object> espaceUtilisateur(Integer utilisateurId) {
        autoriser(utilisateurId);
        Map<String, Object> vue = new HashMap<>();
        Employe employe = employes.findByUtilisateurId(utilisateurId).orElse(null);
        vue.put("employePointage", employe);
        if (employe == null) return vue;
        employe = employes.findByIdForUpdate(employe.getId()).orElseThrow();
        List<BilanPointage> jours = new ArrayList<>();
        for (LocalDate date = aujourdhui(); !date.isBefore(aujourdhui().minusDays(6)); date = date.minusDays(1)) {
            if (affectation(employe, date).active() || !pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employe.getId(), date).isEmpty()) jours.add(bilan(employe, date));
        }
        LocalDate lundi = aujourdhui().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> semaine = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = lundi.plusDays(i); Map<String, Object> jour = new HashMap<>();
            jour.put("date", date); jour.put("libelle", date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH));
            jour.put("planning", planningPour(employe, date)); semaine.add(jour);
        }
        vue.put("joursPointage", jours); vue.put("semainePlanning", semaine);
        vue.put("alertesPointage", alertes.findByEmploye_IdAndResolueLeIsNullOrderByDateDesc(employe.getId()));
        return vue;
    }

    private void verifierPeriode(LocalDate du, LocalDate au) {
        if (du == null || au == null || au.isBefore(du) || Duration.between(du.atStartOfDay(), au.atStartOfDay()).toDays() > 30) {
            throw new IllegalArgumentException("Choisissez une période de 31 jours maximum avec une fin après le début.");
        }
    }

    public List<BilanPointage> rapportRh(Integer acteur, LocalDate du, LocalDate au) {
        autoriser(acteur, "RH"); verifierPeriode(du, au);
        List<BilanPointage> resultats = new ArrayList<>();
        for (Employe employe : employes.findAll(Sort.by("id"))) {
            employes.findByIdForUpdate(employe.getId()).orElseThrow();
            for (LocalDate date = du; !date.isAfter(au); date = date.plusDays(1)) {
                if (affectation(employe, date).active() || !pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employe.getId(), date).isEmpty()
                        || variables.findByEmploye_IdAndDate(employe.getId(), date).isPresent()) resultats.add(bilan(employe, date));
            }
        }
        return resultats;
    }

    public void corriger(Integer acteur, Long pointageId, Long revision, LocalDateTime entree, LocalDateTime sortie, String motif) {
        Utilisateur rh = autoriser(acteur, "RH");
        Integer employeId = pointages.employePourPointage(pointageId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Employe employe = employes.findByIdForUpdate(employeId).orElseThrow();
        Pointage pointage = pointages.findById(pointageId).orElseThrow();
        if (!Objects.equals(revision, pointage.getRevision())) throw new IllegalArgumentException("Le pointage a changé. Rechargez la page.");
        if (variables.findByEmploye_IdAndDate(employeId, pointage.getDate()).isPresent()) throw new IllegalArgumentException("Cette journée a déjà été transmise à Finance.");
        if (motif == null || motif.isBlank() || motif.length() > 1000) throw new IllegalArgumentException("Précisez un motif de correction de 1 à 1 000 caractères.");
        Planning planning = pointage.getPlanning() == null ? planningPour(employe, pointage.getDate()) : pointage.getPlanning();
        var plage = CalculPointage.creneau(pointage.getDate(), planning);
        boolean jourEntreeValide = entree != null && (entree.toLocalDate().equals(pointage.getDate())
                || (plage != null && plage.fin().toLocalDate().isAfter(pointage.getDate())
                    && entree.toLocalDate().equals(plage.fin().toLocalDate()) && !entree.isAfter(plage.fin())));
        if (entree == null || sortie == null || sortie.isBefore(entree) || sortie.isAfter(maintenant())
                || Duration.between(entree, sortie).compareTo(Duration.ofHours(24)) > 0
                || !jourEntreeValide) {
            throw new IllegalArgumentException("Renseignez une entrée du jour concerné et une sortie cohérente, sans date future et sur 24 heures maximum.");
        }
        CorrectionPointage correction = new CorrectionPointage(); correction.setPointage(pointage); correction.setRh(rh);
        correction.setAncienneEntree(pointage.getEntreeEffective()); correction.setAncienneSortie(pointage.getSortieEffective());
        correction.setNouvelleEntree(entree); correction.setNouvelleSortie(sortie); correction.setMotif(motif.trim()); correction.setCorrigeLe(maintenant());
        corrections.save(correction);
        pointage.setConnexionRetenue(entree); pointage.setDeconnexionRetenue(sortie); pointage.setRegularise(true);
        pointages.saveAndFlush(pointage); bilan(employe, pointage.getDate());
    }

    public VariablePaiePointage transmettre(Integer acteur, Integer employeId, LocalDate date) {
        Utilisateur rh = autoriser(acteur, "RH");
        Employe employe = employes.findByIdForUpdate(employeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var precedente = variables.findByEmploye_IdAndDate(employeId, date);
        if (precedente.isPresent()) return precedente.get();
        BilanPointage bilan = bilan(employe, date);
        if (!bilan.isTransmissible()) throw new IllegalArgumentException("Seule une journée passée, terminée, avec planning et sans anomalie bloquante peut être transmise.");
        VariablePaiePointage variable = new VariablePaiePointage();
        variable.setEmploye(employe); variable.setDate(date); variable.setPlanning(bilan.getPlanning());
        variable.setPrevuSecondes(bilan.getPrevuSecondes()); variable.setTravailSecondes(bilan.getTravailSecondes());
        variable.setRetardSecondes(bilan.getRetardSecondes()); variable.setSupplementairesSecondes(bilan.getSupplementairesSecondes());
        variable.setAbsenceSecondes(bilan.getAbsenceSecondes()); variable.setRh(rh); variable.setTransmisLe(maintenant());
        return variables.saveAndFlush(variable);
    }

    public List<VariablePaiePointage> variablesFinance(Integer acteur, LocalDate du, LocalDate au) {
        autoriser(acteur, "FINANCE", "RH"); verifierPeriode(du, au);
        return variables.findByDateBetweenOrderByDateAscEmploye_IdAsc(du, au);
    }

    public void detecterAnomalies() {
        for (Employe employe : employes.findAll(Sort.by("id"))) {
            employes.findByIdForUpdate(employe.getId()).orElseThrow();
            Set<LocalDate> dates = new TreeSet<>(pointages.datesOuvertes(employe.getId()));
            for (int i = 0; i < 7; i++) dates.add(aujourdhui().minusDays(i));
            for (LocalDate date : dates) {
                if (!date.isBefore(employe.getDateEmbauche()) && (affectation(employe, date).active()
                        || !pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employe.getId(), date).isEmpty())) bilan(employe, date);
            }
        }
    }
}
