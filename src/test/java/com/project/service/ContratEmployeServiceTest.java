package com.project.service;

import com.project.entity.ConditionTravail;
import com.project.entity.Contrat;
import com.project.entity.Employe;
import com.project.entity.TypeContrat;
import com.project.repository.ContratRepository;
import com.project.repository.EmployeRepository;
import com.project.repository.TypeContratRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContratEmployeServiceTest {
    private ContratRepository contrats;
    private EmployeRepository employes;
    private TypeContratRepository types;
    private ContratEmployeService service;
    private Employe employe;
    private Contrat contratActuel;
    private TypeContrat cdi;
    private TypeContrat ce;

    @BeforeEach
    void preparer() {
        contrats = mock(ContratRepository.class);
        employes = mock(EmployeRepository.class);
        types = mock(TypeContratRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T09:00:00Z"),
                ZoneId.of("Indian/Antananarivo"));
        service = new ContratEmployeService(contrats, employes, types, mock(JdbcTemplate.class), clock);

        ConditionTravail condition = new ConditionTravail();
        condition.setId(7);
        contratActuel = new Contrat();
        contratActuel.setId(10);
        contratActuel.setCondition(condition);
        employe = new Employe();
        employe.setId(2);
        employe.setContratId(10);

        cdi = new TypeContrat();
        cdi.setId(2);
        cdi.setCode("CDI");
        ce = new TypeContrat();
        ce.setId(1);
        ce.setCode("CE");

        when(employes.findByIdForUpdate(2)).thenReturn(Optional.of(employe));
        when(contrats.findById(10)).thenReturn(Optional.of(contratActuel));
        when(contrats.save(any(Contrat.class))).thenAnswer(invocation -> {
            Contrat contrat = invocation.getArgument(0);
            if (contrat.getId() == null) contrat.setId(99);
            return contrat;
        });
    }

    @Test
    void unCdiImmediatDevientLeContratCourantSansDuree() {
        when(types.findByCode("CDI")).thenReturn(Optional.of(cdi));

        Contrat contrat = service.programmer(2, "CDI", LocalDate.of(2026, 11, 8), null, 5);

        assertEquals(Contrat.STATUT_FINALISE, contrat.getStatut());
        assertEquals(2, contrat.getIdEmploye());
        assertSame(cdi, contrat.getTypeContrat());
        assertSame(contratActuel.getCondition(), contrat.getCondition());
        assertNull(contrat.getDureeMois());
        assertNull(contrat.getDateFin());
        assertEquals(99, employe.getContratId());
        verify(employes).save(employe);
    }

    @Test
    void unCeFuturEstProgrammeAvecSaDuree() {
        when(types.findByCode("CE")).thenReturn(Optional.of(ce));

        Contrat contrat = service.programmer(2, "ce", LocalDate.of(2026, 12, 16), 3, 5);

        assertEquals(Contrat.STATUT_PROGRAMME, contrat.getStatut());
        assertEquals(LocalDate.of(2026, 12, 16), contrat.getDateDebut());
        assertEquals(LocalDate.of(2027, 3, 16), contrat.getDateFin());
        assertEquals(3, contrat.getDureeMois());
        assertEquals(10, employe.getContratId());
        verify(employes, never()).save(employe);
    }

    @Test
    void unCeSansDureeEstRefuse() {
        when(types.findByCode("CE")).thenReturn(Optional.of(ce));

        assertThrows(IllegalArgumentException.class,
                () -> service.programmer(2, "CE", LocalDate.of(2026, 12, 16), null, 5));
    }

    @Test
    void unContratProgrammeDevientCourantALaDateAtteinte() {
        Contrat programme = new Contrat();
        programme.setId(99);
        programme.setIdEmploye(2);
        programme.setStatut(Contrat.STATUT_PROGRAMME);
        when(contrats.findByStatutAndDateDebutLessThanEqualOrderByDateDebutAscIdAsc(
                Contrat.STATUT_PROGRAMME, LocalDate.of(2026, 11, 8)))
                .thenReturn(List.of(programme));

        int actives = service.activerContratsDus();

        assertEquals(1, actives);
        assertEquals(Contrat.STATUT_FINALISE, programme.getStatut());
        assertEquals(99, employe.getContratId());
        verify(employes).save(employe);
    }
}
