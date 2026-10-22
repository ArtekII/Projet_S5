
package com.project.service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.Candidature;
import com.project.entity.CandidatureEtape;
import com.project.entity.CandidatsSelec;
import com.project.entity.Cv;
import com.project.entity.CvCandidat;
import com.project.entity.Entretien;
import com.project.entity.Etape;
import com.project.entity.EtapeRecrutement;
import com.project.entity.OffreEmploi;
import com.project.entity.Resultat;
import com.project.entity.Utilisateur;
import com.project.repository.CandidatureEtapeRepository;
import com.project.repository.CandidatureRepository;
import com.project.repository.CandidatsSelecRepository;
import com.project.repository.CvCandidatRepository;
import com.project.repository.CvRepository;
import com.project.repository.EtapeRecrutementRepository;
import com.project.repository.EtapeRepository;
import com.project.repository.EntretienRepository;
import com.project.repository.OffreEmploiRepository;
import com.project.repository.ResultatRepository;
import com.project.repository.QuestionnaireRepository;
import com.project.repository.StatutCandidatureRepository;
import com.project.repository.UtilisateurRepository;

@Service 
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final CvCandidatRepository cvCandidatRepository;
    private final CvRepository cvRepository;
    private final OffreEmploiRepository offreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EtapeRecrutementRepository etapeRecrutementRepository;
    private final StatutCandidatureRepository statutCandidatureRepository;
    private final CandidatureEtapeRepository candidatureEtapeRepository;
    private final ResultatRepository resultatRepository;
    private final CandidatsSelecRepository candidatsSelecRepository;
    private final EtapeRepository etapeRepository;
    private final EntretienRepository entretienRepository;
    private final QuestionnaireRepository questionnaireRepository;

    public CandidatureService(CandidatureRepository candidatureRepository,
            CvCandidatRepository cvCandidatRepository,
            CvRepository cvRepository,
            OffreEmploiRepository offreRepository,
            UtilisateurRepository utilisateurRepository,
            EtapeRecrutementRepository etapeRecrutementRepository,
            StatutCandidatureRepository statutCandidatureRepository,
            CandidatureEtapeRepository candidatureEtapeRepository,
            ResultatRepository resultatRepository,
            CandidatsSelecRepository candidatsSelecRepository,
            EtapeRepository etapeRepository,
            EntretienRepository entretienRepository,
            QuestionnaireRepository questionnaireRepository) {
        this.candidatureRepository = candidatureRepository;
        this.cvCandidatRepository = cvCandidatRepository;
        this.cvRepository = cvRepository;
        this.offreRepository = offreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.etapeRecrutementRepository = etapeRecrutementRepository;
        this.statutCandidatureRepository = statutCandidatureRepository;
        this.candidatureEtapeRepository = candidatureEtapeRepository;
        this.resultatRepository = resultatRepository;
        this.candidatsSelecRepository = candidatsSelecRepository;
        this.etapeRepository = etapeRepository;
        this.entretienRepository = entretienRepository;
        this.questionnaireRepository = questionnaireRepository;
    }

    public List<Candidature> getCandidaturesByCandidatId(Integer userId) {
        return candidatureRepository.findByCandidat_IdOrderByCreatedAtDesc(userId);
    }

    public List<Candidature> getCandidaturesAvecDetails(Integer userId) {
        return candidatureRepository.findByCandidatIdAvecDetails(userId);
    }

    /**
     * Postulation : le candidat depose son CV pour une offre.
     * La candidature est creee a l'etape 1 (selection des CV) et attend la
     * validation du RH.
     */
    @Transactional
    public Candidature postuler(Integer idCandidat, Integer idOffre, String nom,
            String prenom, LocalDate dateNaissance, Integer anneeExperience,
            String email, String diplome, String competences) {

        if (candidatureRepository.findByOffre_IdAndCandidat_Id(idOffre, idCandidat).isPresent()) {
            throw new IllegalStateException("Vous avez deja postule a cette offre.");
        }

        Utilisateur candidat = utilisateurRepository.findById(idCandidat)
                .orElseThrow(() -> new IllegalStateException("Candidat introuvable."));
        OffreEmploi offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new IllegalStateException("Offre introuvable."));

        // CV depose par le candidat (table cv_candidat)
        CvCandidat cvCandidat = cvCandidatRepository
                .findByCandidat_IdAndOffreEmploi_Id(idCandidat, idOffre)
                .orElseGet(CvCandidat::new);
        cvCandidat.setCandidat(candidat);
        cvCandidat.setOffreEmploi(offre);
        cvCandidat.setNom(nom);
        cvCandidat.setPrenom(prenom);
        cvCandidat.setDateNaissance(dateNaissance);
        cvCandidat.setAnneeExperience(anneeExperience == null ? 0 : anneeExperience);
        cvCandidat.setEmail(email);
        cvCandidat.setCreatedAt(LocalDateTime.now());
        cvCandidatRepository.save(cvCandidat);

        // Cursus complet (table cv) : reference obligatoire de la candidature
        Cv cv = new Cv();
        cv.setCandidat(candidat);
        cv.setNom(nom);
        cv.setPrenom(prenom);
        cv.setDateNaissance(dateNaissance == null ? LocalDate.now() : dateNaissance);
        cv.setDiplome(diplome == null || diplome.isBlank() ? "Non renseigne" : diplome);
        cv.setCompetences(competences == null || competences.isBlank() ? "Non renseigne"
                : competences);
        cv.setCreatedAt(LocalDateTime.now());
        cvRepository.save(cv);

        // Candidature : etape 1, en attente de validation RH
        Candidature candidature = new Candidature();
        candidature.setOffre(offre);
        candidature.setCandidat(candidat);
        candidature.setCv(cv);
        candidature.setEtape(
                etapeRecrutementRepository.findByCode("CV").orElseThrow());
        candidature.setStatut(
                statutCandidatureRepository.findByCode("EN_COURS").orElseThrow());
        candidature.setCreatedAt(LocalDateTime.now());
        return candidatureRepository.save(candidature);
    }

    /**
     * Le RH valide l'etape en cours de la candidature : l'etape est notee dans
     * l'historique et la candidature passe a l'etape suivante.
     */
    @Transactional
    public Candidature validerEtape(Integer idCandidature, String codeEtapeSuivante,
            Utilisateur decideur) {

        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new IllegalStateException("Candidature introuvable."));

        EtapeRecrutement etapeEnCours = candidature.getEtape();
        verifierEtapeMetier(candidature, etapeEnCours);
        Resultat reussi = resultatRepository.findByCode("REUSSI").orElseThrow();
        enregistrerSelection(candidature, etapeEnCours);

        CandidatureEtape historique = new CandidatureEtape();
        historique.setCandidature(candidature);
        historique.setEtape(etapeEnCours);
        historique.setResultat(reussi);
        historique.setDecideur(decideur);
        historique.setCreatedAt(LocalDateTime.now());
        candidatureEtapeRepository.save(historique);

        if (codeEtapeSuivante != null && !codeEtapeSuivante.isBlank()) {
            candidature.setEtape(etapeRecrutementRepository.findByCode(codeEtapeSuivante)
                    .orElseThrow(() -> new IllegalStateException(
                            "Etape inconnue : " + codeEtapeSuivante)));
        }
        return candidatureRepository.save(candidature);
    }

    /**
     * Valide l'etape en cours et fait passer la candidature a l'etape suivante
     * du processus (ordre de {@code etape_recrutement}).
     */
    @Transactional
    public Candidature validerEtapeSuivante(Integer idCandidature, Utilisateur decideur) {
        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new IllegalStateException("Candidature introuvable."));
        EtapeRecrutement etapeEnCours = candidature.getEtape();
        String codeSuivant = null;
        if (etapeEnCours != null) {
            Optional<EtapeRecrutement> suivante = etapeRecrutementRepository
                    .findFirstByOrdreGreaterThanOrderByOrdreAsc(etapeEnCours.getOrdre());
            if (suivante.isPresent()) {
                codeSuivant = suivante.get().getCode();
            }
        }
        return validerEtape(idCandidature, codeSuivant, decideur);
    }

    /** Le RH elimine la candidature (etape en cours non validee). */
    @Transactional
    public Candidature eliminer(Integer idCandidature, Utilisateur decideur) {
        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new IllegalStateException("Candidature introuvable."));

        Resultat echoue = resultatRepository.findByCode("ECHOUE").orElseThrow();
        CandidatureEtape historique = new CandidatureEtape();
        historique.setCandidature(candidature);
        historique.setEtape(candidature.getEtape());
        historique.setResultat(echoue);
        historique.setDecideur(decideur);
        historique.setCreatedAt(LocalDateTime.now());
        candidatureEtapeRepository.save(historique);

        candidature.setStatut(
                statutCandidatureRepository.findByCode("ELIMINE").orElseThrow());
        return candidatureRepository.save(candidature);
    }

    /**
     * Enregistre l'evaluation d'entretien. L'entretien est lie a la candidature
     * du portail et sa note est aussi recopiee dans candidats_selec.
     */
    @Transactional
    public Entretien enregistrerEntretien(Integer idCandidature, Utilisateur rh,
            LocalDateTime dateEntretien, String lieu,
            BigDecimal noteHonneteteCv, BigDecimal notePersonnalite,
            BigDecimal noteCompetences) {
        verifierNote(noteHonneteteCv);
        verifierNote(notePersonnalite);
        verifierNote(noteCompetences);
        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new IllegalStateException("Candidature introuvable."));
        if (candidature.getEtape() == null
                || !"ENTRETIEN".equals(candidature.getEtape().getCode())) {
            throw new IllegalStateException("La candidature n'est pas à l'étape entretien.");
        }
        if (dateEntretien == null || lieu == null || lieu.isBlank()) {
            throw new IllegalArgumentException("La date, l'heure et le lieu de l'entretien sont obligatoires.");
        }

        Entretien entretien = entretienRepository.findByCandidature_Id(idCandidature)
                .orElseGet(Entretien::new);
        entretien.setCandidature(candidature);
        entretien.setRh(rh);
        entretien.setDateEntretien(dateEntretien);
        entretien.setLieu(lieu.trim());
        entretien.setNoteHonneteteCv(noteHonneteteCv);
        entretien.setNotePersonnalite(notePersonnalite);
        entretien.setNoteCompetences(noteCompetences);
        Entretien saved = entretienRepository.save(entretien);
        enregistrerSelection(candidature, candidature.getEtape());
        recalculerClassement(candidature.getOffre().getId(), "Entretien");
        return saved;
    }

    public Optional<Entretien> getEntretien(Integer idCandidature) {
        return entretienRepository.findByCandidature_Id(idCandidature);
    }

    /** Candidatures en cours de traitement, pour le tableau de bord RH. */
    public List<Candidature> getCandidaturesEnCours() {
        return candidatureRepository.findAllEnCoursAvecDetails();
    }

    /** Toutes les candidatures d'une offre, y compris celles deja eliminees. */
    public List<Candidature> getCandidaturesByOffre(Integer idOffre) {
        return candidatureRepository.findByOffreIdAvecDetails(idOffre);
    }

    /** Etapes deja validees par le RH pour une candidature. */
    public List<CandidatureEtape> getEtapesFranchies(Integer idCandidature) {
        return candidatureEtapeRepository
                .findByCandidature_IdOrderByCreatedAtAsc(idCandidature);
    }

    public Optional<Candidature> findCandidature(Integer idCandidature) {
        return candidatureRepository.findById(idCandidature);
    }

    private void verifierEtapeMetier(Candidature candidature, EtapeRecrutement etape) {
        if (etape == null) {
            throw new IllegalStateException("Etape de candidature absente.");
        }
        if ("QUESTIONNAIRE".equals(etape.getCode())) {
            BigDecimal score = candidature.getScoreQuestionnaire();
            BigDecimal seuil = questionnaireRepository.findByOffre_Id(candidature.getOffre().getId())
                    .map(q -> q.getSeuilReussite())
                    .orElse(BigDecimal.ZERO);
            if (score == null || score.compareTo(seuil) < 0) {
                throw new IllegalStateException("Le score du questionnaire est insuffisant.");
            }
        }
        if ("ENTRETIEN".equals(etape.getCode())
                && entretienRepository.findByCandidature_Id(candidature.getId()).isEmpty()) {
            throw new IllegalStateException("L'entretien doit être évalué avant validation.");
        }
    }

    private void enregistrerSelection(Candidature candidature, EtapeRecrutement etapeRecrutement) {
        String nomEtape = switch (etapeRecrutement.getCode()) {
            case "CV" -> "selection";
            case "QUESTIONNAIRE" -> "Test";
            case "ENTRETIEN" -> "Entretien";
            default -> null;
        };
        if (nomEtape == null) {
            return;
        }
        CvCandidat cv = cvCandidatRepository
                .findByCandidat_IdAndOffreEmploi_Id(candidature.getCandidat().getId(),
                        candidature.getOffre().getId())
                .orElseThrow(() -> new IllegalStateException("CV candidat introuvable."));
        Etape etape = etapeRepository.findByNom(nomEtape)
                .orElseGet(() -> etapeRepository.save(new Etape(nomEtape)));
        CandidatsSelec selection = candidatsSelecRepository
                .findByOffreEmploi_IdAndCvCandidat_IdAndEtape_Nom(
                        candidature.getOffre().getId(), cv.getId(), nomEtape)
                .orElseGet(CandidatsSelec::new);
        selection.setOffreEmploi(candidature.getOffre());
        selection.setCvCandidat(cv);
        selection.setEtape(etape);
        selection.setDateSelection(LocalDateTime.now());
        selection.setNote(noteEtape(candidature, etapeRecrutement));
        candidatsSelecRepository.save(selection);
        recalculerClassement(candidature.getOffre().getId(), nomEtape);
    }

    private void recalculerClassement(Integer idOffre, String nomEtape) {
        List<CandidatsSelec> selections = candidatsSelecRepository
                .findByOffreEmploi_IdOrderByRangAscNoteDesc(idOffre).stream()
                .filter(selection -> nomEtape.equals(selection.getEtape().getNom()))
                .sorted((a, b) -> {
                    BigDecimal noteA = a.getNote() == null ? BigDecimal.ZERO : a.getNote();
                    BigDecimal noteB = b.getNote() == null ? BigDecimal.ZERO : b.getNote();
                    return noteB.compareTo(noteA);
                })
                .toList();
        int rang = 1;
        for (CandidatsSelec selection : selections) {
            selection.setRang(rang++);
            candidatsSelecRepository.save(selection);
        }
    }

    private BigDecimal noteEtape(Candidature candidature, EtapeRecrutement etape) {
        if ("QUESTIONNAIRE".equals(etape.getCode())) {
            return candidature.getScoreQuestionnaire();
        }
        if ("ENTRETIEN".equals(etape.getCode())) {
            return entretienRepository.findByCandidature_Id(candidature.getId())
                    .map(Entretien::getNoteTotale).orElse(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private void verifierNote(BigDecimal note) {
        if (note == null || note.compareTo(BigDecimal.ZERO) < 0
                || note.compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new IllegalArgumentException("Chaque note d'entretien doit être comprise entre 0 et 20.");
        }
    }
}
