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
    private final CandidatSelec1Repository selection1Repository;
    private final QuestionTesteRepository questionRepository;
    private final CandidatSelec2Repository selection2Repository;
    private final EntretienQualificationRepository qualificationRepository;
    private final EntretienCandidatRepository entretienRepository;
    private final CandidatSelec3Repository selection3Repository;
    private final OffreService offreService;

    public RecrutementService(CvCandidatRepository cvRepository, CandidatSelec1Repository selection1Repository,
            QuestionTesteRepository questionRepository, CandidatSelec2Repository selection2Repository,
            EntretienQualificationRepository qualificationRepository, EntretienCandidatRepository entretienRepository,
            CandidatSelec3Repository selection3Repository, OffreService offreService) {
        this.cvRepository = cvRepository;
        this.selection1Repository = selection1Repository;
        this.questionRepository = questionRepository;
        this.selection2Repository = selection2Repository;
        this.qualificationRepository = qualificationRepository;
        this.entretienRepository = entretienRepository;
        this.selection3Repository = selection3Repository;
        this.offreService = offreService;
    }

    public List<CvCandidat> candidatures(Integer offreId) {
        return cvRepository.findAll().stream()
                .filter(cv -> cv.getOffre().getId().equals(offreId))
                .sorted(Comparator.comparing(CvCandidat::getAnneeExperience).reversed()
                        .thenComparing(CvCandidat::getCreatedAt))
                .toList();
    }

    public List<CandidatSelec1> selections1(Integer offreId) {
        return selection1Repository.findByOffre_IdOrderByCvCandidat_AnneeExperienceDescCvCandidat_CreatedAtAsc(offreId);
    }

    @Transactional
    public void validerSelection1(Integer offreId, List<Integer> cvIds) {
        OffreEmploi offre = offrePubliee(offreId);
        Set<Integer> selected = cvIds == null ? Set.of() : new HashSet<>(cvIds);
        for (CvCandidat cv : candidatures(offreId)) {
            if (selected.contains(cv.getId())
                    && !selection1Repository.existsByOffre_IdAndCvCandidat_Id(offreId, cv.getId())) {
                CandidatSelec1 selection = new CandidatSelec1();
                selection.setOffre(offre);
                selection.setCvCandidat(cv);
                selection.setDateSelection(LocalDateTime.now());
                selection1Repository.save(selection);
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
        for (CandidatSelec1 selection1 : selections1(offreId)) {
            BigDecimal note = points == null ? null : points.get(selection1.getId());
            if (note == null || note.signum() < 0)
                continue;
            CandidatSelec2 selection2 = selection2Repository.findByCandidatSelec1_Id(selection1.getId())
                    .orElseGet(() -> {
                        CandidatSelec2 created = new CandidatSelec2();
                        created.setCandidatSelec1(selection1);
                        return created;
                    });
            selection2.setPoints(note);
            selection2.setDateSelection(LocalDateTime.now());
            selection2Repository.save(selection2);
        }
    }

    @Transactional
    public List<CandidatSelec2> calculerTop5(Integer offreId) {
        offrePubliee(offreId);
        List<CandidatSelec2> notes = selection2Repository.findByCandidatSelec1_Offre_IdOrderByPointsDesc(offreId);
        List<CandidatSelec2> top5 = notes.stream().limit(5).toList();
        Set<Integer> ids = top5.stream().map(CandidatSelec2::getId).collect(java.util.stream.Collectors.toSet());
        for (CandidatSelec2 note : notes) {
            if (!ids.contains(note.getId()))
                selection2Repository.delete(note);
        }
        return top5;
    }

    public List<CandidatSelec2> top5(Integer offreId) {
        return selection2Repository.findByCandidatSelec1_Offre_IdOrderByPointsDesc(offreId).stream().limit(5).toList();
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
        for (CandidatSelec2 selection2 : top5(offreId)) {
            BigDecimal note = points == null ? null : points.get(selection2.getId());
            if (note == null || note.signum() < 0)
                continue;
            EntretienCandidat resultat = entretienRepository.findByCandidatSelec2_Id(selection2.getId()).stream()
                    .filter(e -> e.getEntretienQualification().getId().equals(entretienId))
                    .findFirst().orElseGet(() -> {
                        EntretienCandidat created = new EntretienCandidat();
                        created.setEntretienQualification(qualification);
                        created.setCandidatSelec2(selection2);
                        return created;
                    });
            resultat.setPoints(note);
            entretienRepository.save(resultat);
        }
    }

    @Transactional
    public List<CandidatSelec3> calculerClassementFinal(Integer offreId) {
        offrePubliee(offreId);
        List<CandidatSelec2> top5 = top5(offreId);
        List<CandidatSelec3> classement = new ArrayList<>();
        for (CandidatSelec2 selection2 : top5) {
            BigDecimal total = entretienRepository.findByCandidatSelec2_Id(selection2.getId()).stream()
                    .map(EntretienCandidat::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
            CandidatSelec3 ligne = selection3Repository.findAll().stream()
                    .filter(v -> v.getCandidatSelec2().getId().equals(selection2.getId()))
                    .findFirst().orElseGet(() -> {
                        CandidatSelec3 created = new CandidatSelec3();
                        created.setCandidatSelec2(selection2);
                        return created;
                    });
            ligne.setNoteFinale(total);
            classement.add(ligne);
        }
        classement.sort(Comparator.comparing(CandidatSelec3::getNoteFinale).reversed());
        for (int i = 0; i < classement.size(); i++) {
            classement.get(i).setRang(i + 1);
            selection3Repository.save(classement.get(i));
        }
        return classement;
    }

    public List<CandidatSelec3> classement(Integer offreId) {
        return selection3Repository.findByCandidatSelec2_CandidatSelec1_Offre_IdOrderByRangAsc(offreId);
    }

    private OffreEmploi offrePubliee(Integer offreId) {
        OffreEmploi offre = offreService.findOffre(offreId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable."));
        if (!"PUBLIEE".equals(offre.getStatut().getCode()))
            throw new IllegalStateException("Cette offre n'est pas publiée.");
        return offre;
    }
}
