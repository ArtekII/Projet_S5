
package com.project.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.Candidature;
import com.project.entity.CandidatureEtape;
import com.project.entity.Cv;
import com.project.entity.CvCandidat;
import com.project.entity.EtapeRecrutement;
import com.project.entity.OffreEmploi;
import com.project.entity.Resultat;
import com.project.entity.StatutCandidature;
import com.project.entity.Utilisateur;
import com.project.repository.CandidatureEtapeRepository;
import com.project.repository.CandidatureRepository;
import com.project.repository.CvCandidatRepository;
import com.project.repository.CvRepository;
import com.project.repository.EtapeRecrutementRepository;
import com.project.repository.OffreEmploiRepository;
import com.project.repository.ResultatRepository;
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

    public CandidatureService(CandidatureRepository candidatureRepository,
            CvCandidatRepository cvCandidatRepository,
            CvRepository cvRepository,
            OffreEmploiRepository offreRepository,
            UtilisateurRepository utilisateurRepository,
            EtapeRecrutementRepository etapeRecrutementRepository,
            StatutCandidatureRepository statutCandidatureRepository,
            CandidatureEtapeRepository candidatureEtapeRepository,
            ResultatRepository resultatRepository) {
        this.candidatureRepository = candidatureRepository;
        this.cvCandidatRepository = cvCandidatRepository;
        this.cvRepository = cvRepository;
        this.offreRepository = offreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.etapeRecrutementRepository = etapeRecrutementRepository;
        this.statutCandidatureRepository = statutCandidatureRepository;
        this.candidatureEtapeRepository = candidatureEtapeRepository;
        this.resultatRepository = resultatRepository;
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
        CvCandidat cvCandidat = new CvCandidat();
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
        Resultat reussi = resultatRepository.findByCode("REUSSI").orElseThrow();

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

    /** Candidatures en cours de traitement, pour le tableau de bord RH. */
    public List<Candidature> getCandidaturesEnCours() {
        return candidatureRepository.findAllEnCoursAvecDetails();
    }

    /** Etapes deja validees par le RH pour une candidature. */
    public List<CandidatureEtape> getEtapesFranchies(Integer idCandidature) {
        return candidatureEtapeRepository
                .findByCandidature_IdOrderByCreatedAtAsc(idCandidature);
    }

    public Optional<Candidature> findCandidature(Integer idCandidature) {
        return candidatureRepository.findById(idCandidature);
    }
}
