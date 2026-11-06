package com.project.config;

import com.project.entity.Contrat;
import com.project.repository.ContratRepository;
import com.project.service.EmployeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeInitializerTest {
    @Test
    void dossierHistoriqueIncompletNeBloquePasLesAutresEmployes() {
        var contrats = mock(ContratRepository.class);
        var employes = mock(EmployeService.class);
        Contrat incomplet = new Contrat(); incomplet.setId(2); incomplet.setStatut("FINALISE");
        Contrat valide = new Contrat(); valide.setId(1); valide.setStatut("FINALISE");
        when(contrats.findAllByOrderByIdDesc()).thenReturn(List.of(incomplet, valide));
        doThrow(new EmployeService.DossierCvManquantException(20)).when(employes).assurerPourContrat(incomplet);
        assertDoesNotThrow(() -> new EmployeInitializer(contrats, employes).run());
        verify(employes).assurerPourContrat(valide);
    }

    @Test
    void erreurBaseDeDonneesResteVisible() {
        var contrats = mock(ContratRepository.class);
        var employes = mock(EmployeService.class);
        Contrat contrat = new Contrat(); contrat.setStatut("FINALISE");
        when(contrats.findAllByOrderByIdDesc()).thenReturn(List.of(contrat));
        doThrow(new IllegalStateException("Erreur SQL")).when(employes).assurerPourContrat(contrat);
        assertThrows(IllegalStateException.class, () -> new EmployeInitializer(contrats, employes).run());
    }
}
