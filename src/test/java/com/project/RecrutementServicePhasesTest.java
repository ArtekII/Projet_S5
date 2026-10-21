package com.project;

import com.project.entity.*;
import com.project.repository.*;
import com.project.service.OffreService;
import com.project.service.RecrutementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecrutementServicePhasesTest {
    @Mock
    private CvCandidatRepository cvRepository;
    @Mock
    private CandidatSelecRepository selectionRepository;
    @Mock
    private QuestionTesteRepository questionRepository;
    @Mock
    private EntretienQualificationRepository qualificationRepository;
    @Mock
    private EntretienCandidatRepository entretienRepository;
    @Mock
    private EtapeRepository etapeRepository;
    @Mock
    private OffreService offreService;

    private RecrutementService service;
    private OffreEmploi offre;
    private Etape selectionEtape;
    private Etape testEtape;
    private Etape entretienEtape;

    @BeforeEach
    void setUp() {
        service = new RecrutementService(cvRepository, selectionRepository, questionRepository,
                qualificationRepository, entretienRepository, etapeRepository, offreService);

        StatutOffre statut = new StatutOffre();
        statut.setCode("PUBLIEE");
        offre = new OffreEmploi();
        offre.setId(10);
        offre.setStatut(statut);

        selectionEtape = etape("selection");
        testEtape = etape("Test");
        entretienEtape = etape("Entretien");
        when(offreService.findOffre(10)).thenReturn(Optional.of(offre));
        when(etapeRepository.findByNom("selection")).thenReturn(Optional.of(selectionEtape));
        when(etapeRepository.findByNom("Test")).thenReturn(Optional.of(testEtape));
        when(etapeRepository.findByNom("Entretien")).thenReturn(Optional.of(entretienEtape));
    }

    @Test
    void selectionPhaseEntryCreatesSelectionRecord() {
        CvCandidat cv = cv(1, 3);
        when(cvRepository.findAll()).thenReturn(List.of(cv));
        when(selectionRepository.findByOffre_IdAndCvCandidat_IdAndEtape_Nom(10, 1, "selection"))
                .thenReturn(Optional.empty());

        service.validerSelection1(10, List.of(1));

        ArgumentCaptor<CandidatSelec> captor = ArgumentCaptor.forClass(CandidatSelec.class);
        verify(selectionRepository).save(captor.capture());
        CandidatSelec saved = captor.getValue();
        assertSame(offre, saved.getOffre());
        assertSame(cv, saved.getCvCandidat());
        assertSame(selectionEtape, saved.getEtape());
        assertNotNull(saved.getDateSelection());
    }

    @Test
    void testPhaseEntryAndExitKeepsOnlyTopFive() {
        CandidatSelec selection = selection(101, cv(1, 2), selectionEtape, BigDecimal.ZERO);
        when(selectionRepository
                .findByOffre_IdAndEtape_NomOrderByCvCandidat_AnneeExperienceDescCvCandidat_CreatedAtAsc(
                        10, "selection"))
                .thenReturn(List.of(selection));
        when(selectionRepository.findByOffre_IdAndCvCandidat_IdAndEtape_Nom(10, 1, "Test"))
                .thenReturn(Optional.empty());

        service.enregistrerPointsQuestionnaire(10, Map.of(101, new BigDecimal("18.50")));

        ArgumentCaptor<CandidatSelec> entry = ArgumentCaptor.forClass(CandidatSelec.class);
        verify(selectionRepository).save(entry.capture());
        assertSame(testEtape, entry.getValue().getEtape());
        assertEquals(new BigDecimal("18.50"), entry.getValue().getNote());

        List<CandidatSelec> sixTests = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixTests.add(selection(200 + i, cv(20 + i, i), testEtape,
                    BigDecimal.valueOf(20 - i)));
        }
        when(selectionRepository.findByOffre_IdAndEtape_NomOrderByNoteDesc(10, "Test"))
                .thenReturn(sixTests);

        List<CandidatSelec> top5 = service.calculerTop5(10);

        assertEquals(5, top5.size());
        verify(selectionRepository).delete(sixTests.get(5));
    }

    @Test
    void entretienPhaseEntryAndExitCreatesFinalRankedRecord() {
        CandidatSelec testSelection = selection(301, cv(3, 4), testEtape, new BigDecimal("16"));
        EntretienQualification qualification = mock(EntretienQualification.class);
        when(qualification.getId()).thenReturn(50);
        when(qualification.getOffre()).thenReturn(offre);
        when(qualificationRepository.findById(50)).thenReturn(Optional.of(qualification));
        when(selectionRepository.findByOffre_IdAndEtape_NomOrderByNoteDesc(10, "Test"))
                .thenReturn(List.of(testSelection));
        when(entretienRepository.findByCandidatSelec_Id(301)).thenReturn(List.of());

        service.enregistrerNotesEntretien(10, 50, Map.of(301, new BigDecimal("14")));

        ArgumentCaptor<EntretienCandidat> interviewCaptor = ArgumentCaptor.forClass(EntretienCandidat.class);
        verify(entretienRepository).save(interviewCaptor.capture());
        assertSame(testSelection, interviewCaptor.getValue().getCandidatSelec());
        assertEquals(new BigDecimal("14"), interviewCaptor.getValue().getPoints());

        EntretienCandidat result = mock(EntretienCandidat.class);
        when(result.getPoints()).thenReturn(new BigDecimal("14"));
        when(entretienRepository.findByCandidatSelec_Id(301)).thenReturn(List.of(result));
        when(selectionRepository.findByOffre_IdAndCvCandidat_IdAndEtape_Nom(10, 3, "Entretien"))
                .thenReturn(Optional.empty());

        List<CandidatSelec> classement = service.calculerClassementFinal(10);

        assertEquals(1, classement.size());
        assertSame(entretienEtape, classement.get(0).getEtape());
        assertEquals(new BigDecimal("14"), classement.get(0).getNote());
        assertEquals(1, classement.get(0).getRang());
        verify(selectionRepository).save(classement.get(0));
    }

    private CvCandidat cv(int id, int experience) {
        CvCandidat cv = mock(CvCandidat.class);
        when(cv.getId()).thenReturn(id);
        when(cv.getAnneeExperience()).thenReturn(experience);
        when(cv.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(cv.getOffre()).thenReturn(offre);
        return cv;
    }

    private CandidatSelec selection(int id, CvCandidat cv, Etape etape, BigDecimal note) {
        CandidatSelec selection = mock(CandidatSelec.class);
        when(selection.getId()).thenReturn(id);
        when(selection.getCvCandidat()).thenReturn(cv);
        when(selection.getOffre()).thenReturn(offre);
        when(selection.getEtape()).thenReturn(etape);
        when(selection.getNote()).thenReturn(note);
        return selection;
    }

    private Etape etape(String nom) {
        Etape etape = new Etape();
        etape.setNom(nom);
        return etape;
    }
}
