package com.project.service;

import com.project.entity.*;
import com.project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecrutementService {
    private final CvCandidatRepository cvRepository;
    private final CandidatSelecRepository selectionRepository;
    private final QuestionTesteRepository questionRepository;
    private final EntretienQualificationRepository qualificationRepository;
    private final EntretienCandidatRepository entretienRepository;
    private final EtapeRepository etapeRepository;
    private final OffreService offreService;

    public RecrutementService(CvCandidatRepository cvRepository, CandidatSelecRepository selectionRepository,
            QuestionTesteRepository questionRepository, EntretienQualificationRepository qualificationRepository,
            EntretienCandidatRepository entretienRepository, EtapeRepository etapeRepository,
            OffreService offreService) {
        this.cvRepository = cvRepository;
        this.selectionRepository = selectionRepository;
        this.questionRepository = questionRepository;
        this.qualificationRepository = qualificationRepository;
        this.entretienRepository = entretienRepository;
        this.etapeRepository = etapeRepository;
        this.offreService = offreService;
    }

    public List<CvCandidat> candidatures(Integer offreId) {
        return cvRepository.findAll().stream()
                .filter(cv -> cv.getOffre().getId().equals(offreId))
                .sorted(Comparator.comparing(CvCandidat::getAnneeExperience).reversed()
                        .thenComparing(CvCandidat::getCreatedAt))
                .toList();
    }

    public List<CandidatSelec> selections1(Integer offreId) {
        return selectionRepository
                .findByOffre_IdAndEtape_NomOrderByCvCandidat_AnneeExperienceDescCvCandidat_CreatedAtAsc(
                        offreId, "selection");
    }

    @Transactional
    public void validerSelection1(Integer offreId, List<Integer> cvIds) {
        OffreEmploi offre = offrePubliee(offreId);
        Set<Integer> selected = cvIds == null ? Set.of() : new HashSet<>(cvIds);
        Etape etape = etape("selection");
        for (CvCandidat cv : candidatures(offreId)) {
            if (selected.contains(cv.getId())
                    && selectionRepository
                            .findByOffre_IdAndCvCandidat_IdAndEtape_Nom(offreId, cv.getId(), "selection")
                            .isEmpty()) {
                CandidatSelec selection = new CandidatSelec();
                selection.setOffre(offre);
                selection.setCvCandidat(cv);
                selection.setEtape(etape);
                selection.setDateSelection(LocalDateTime.now());
                selectionRepository.save(selection);
            }
        }
    }

    public List<QuestionTeste> questions(Integer offreId) {
        return questionRepository.findByOffre_IdOrderByIdAsc(offreId);
    }

    @Transactional
    public void ajouterQuestion(Integer offreId, String texte, Integer pointsMax) {
        OffreEmploi offre = offrePubliee(offreId);
        if (texte == null || texte.isBlank() || pointsMax == null || pointsMax <= 0)
            throw new IllegalArgumentException("Question et points maximum obligatoires.");
        QuestionTeste question = new QuestionTeste();
        question.setOffre(offre);
        question.setQuestion(texte.trim());
        question.setPointsMax(pointsMax);
        questionRepository.save(question);
    }

    public boolean candidatSelectionne(Integer candidatId, Integer offreId) {
        return selections1(offreId).stream()
                .anyMatch(s -> s.getCvCandidat().getCandidat().getId().equals(candidatId));
    }

    @Transactional
    public void enregistrerPointsQuestionnaire(Integer offreId, Map<Integer, BigDecimal> points) {
        offrePubliee(offreId);
        Etape etapeTest = etape("Test");
        for (CandidatSelec selection : selections1(offreId)) {
            BigDecimal note = points == null ? null : points.get(selection.getId());
            if (note == null || note.signum() < 0)
                continue;
            CandidatSelec test = selectionRepository
                    .findByOffre_IdAndCvCandidat_IdAndEtape_Nom(offreId, selection.getCvCandidat().getId(), "Test")
                    .orElseGet(() -> {
                        CandidatSelec created = new CandidatSelec();
                        created.setOffre(selection.getOffre());
                        created.setCvCandidat(selection.getCvCandidat());
                        created.setEtape(etapeTest);
                        created.setDateSelection(LocalDateTime.now());
                        return created;
                    });
            test.setNote(note);
            selectionRepository.save(test);
        }
    }

    @Transactional
    public List<CandidatSelec> calculerTop5(Integer offreId) {
        offrePubliee(offreId);
        List<CandidatSelec> notes = top5Source(offreId);
        List<CandidatSelec> top5 = notes.stream().limit(5).toList();
        Set<Integer> ids = top5.stream().map(CandidatSelec::getId).collect(java.util.stream.Collectors.toSet());
        notes.stream().filter(note -> !ids.contains(note.getId())).forEach(selectionRepository::delete);
        return top5;
    }

    public List<CandidatSelec> top5(Integer offreId) {
        return top5Source(offreId).stream().limit(5).toList();
    }

    public List<EntretienQualification> entretiens(Integer offreId) {
        return qualificationRepository.findByOffre_IdOrderByIdAsc(offreId);
    }

    @Transactional
    public void ajouterEntretien(Integer offreId, String nom) {
        OffreEmploi offre = offrePubliee(offreId);
        if (nom == null || nom.isBlank())
            throw new IllegalArgumentException("Le nom de l'entretien est obligatoire.");
        EntretienQualification entretien = new EntretienQualification();
        entretien.setOffre(offre);
        entretien.setNom(nom.trim());
        qualificationRepository.save(entretien);
    }

    @Transactional
    public void enregistrerNotesEntretien(Integer offreId, Integer entretienId, Map<Integer, BigDecimal> points) {
        offrePubliee(offreId);
        EntretienQualification qualification = qualificationRepository.findById(entretienId)
                .filter(e -> e.getOffre().getId().equals(offreId))
                .orElseThrow(() -> new IllegalArgumentException("Entretien introuvable."));
        for (CandidatSelec selection : top5(offreId)) {
            BigDecimal note = points == null ? null : points.get(selection.getId());
            if (note == null || note.signum() < 0)
                continue;
            EntretienCandidat resultat = entretienRepository.findByCandidatSelec_Id(selection.getId()).stream()
                    .filter(e -> e.getEntretienQualification().getId().equals(entretienId))
                    .findFirst().orElseGet(() -> {
                        EntretienCandidat created = new EntretienCandidat();
                        created.setEntretienQualification(qualification);
                        created.setCandidatSelec(selection);
                        return created;
                    });
            resultat.setPoints(note);
            entretienRepository.save(resultat);
        }
    }

    @Transactional
    public List<CandidatSelec> calculerClassementFinal(Integer offreId) {
        offrePubliee(offreId);
        List<CandidatSelec> classement = new ArrayList<>();
        for (CandidatSelec selection : top5(offreId)) {
            BigDecimal total = entretienRepository.findByCandidatSelec_Id(selection.getId()).stream()
                    .map(EntretienCandidat::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
            CandidatSelec ligne = selectionRepository
                    .findByOffre_IdAndCvCandidat_IdAndEtape_Nom(offreId, selection.getCvCandidat().getId(), "Entretien")
                    .orElseGet(() -> {
                        CandidatSelec created = new CandidatSelec();
                        created.setOffre(selection.getOffre());
                        created.setCvCandidat(selection.getCvCandidat());
                        created.setEtape(etape("Entretien"));
                        created.setDateSelection(LocalDateTime.now());
                        return created;
                    });
            ligne.setNote(total);
            classement.add(ligne);
        }
        classement.sort(Comparator.comparing(CandidatSelec::getNote).reversed());
        for (int i = 0; i < classement.size(); i++) {
            classement.get(i).setRang(i + 1);
            selectionRepository.save(classement.get(i));
        }
        return classement;
    }

    public List<CandidatSelec> classement(Integer offreId) {
        return selectionRepository.findByOffre_IdAndEtape_NomOrderByRangAsc(offreId, "Entretien");
    }

    private List<CandidatSelec> top5Source(Integer offreId) {
        return selectionRepository.findByOffre_IdAndEtape_NomOrderByNoteDesc(offreId, "Test");
    }

    private Etape etape(String nom) {
        return etapeRepository.findByNom(nom)
                .orElseThrow(() -> new IllegalStateException("Etape introuvable : " + nom));
    }

    private OffreEmploi offrePubliee(Integer offreId) {
        OffreEmploi offre = offreService.findOffre(offreId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable."));
        if (!"PUBLIEE".equals(offre.getStatut().getCode()))
            throw new IllegalStateException("Cette offre n'est pas publiée.");
        return offre;
    }
}
