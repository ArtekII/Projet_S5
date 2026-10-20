package com.project.service;

import com.project.entity.*;
import com.project.repository.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionTravailServiceTest {
    @Mock ConditionTravailRepository conditions;
    @Mock PosteRepository postes;
    @Mock CandidatureRepository candidatures;
    @Mock RoleRepository roles;
    @Mock DecisionRepository decisions;
    @Mock ReponseConditionRepository reponses;
    @InjectMocks ConditionTravailService service;

    private ConditionTravail donnees() {
        ConditionTravail c = new ConditionTravail();
        c.setSalaire(new BigDecimal("1000.00"));
        c.setHeuresParSemaine(new BigDecimal("40.0"));
        c.setLieuTravail("Antananarivo");
        return c;
    }

    private Role references(String code) {
        Role role = new Role();
        role.setCode(code);
        when(roles.findByCode(code)).thenReturn(Optional.of(role));
        Decision attente = new Decision();
        attente.setCode("EN_ATTENTE");
        when(decisions.findByCode("EN_ATTENTE")).thenReturn(Optional.of(attente));
        when(conditions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return role;
    }

    @Test
    void definitUneNouvelleVersionDuPosteSansReprendreLesIdentifiantsEntrants() {
        Poste poste = new Poste();
        poste.setId(2);
        when(postes.findByIdForUpdate(2)).thenReturn(Optional.of(poste));
        ConditionTravail ancienne = donnees();
        ancienne.setVersion(3);
        when(conditions.findFirstByPoste_IdOrderByVersionDesc(2)).thenReturn(Optional.of(ancienne));
        Role rh = references("RH");
        ConditionTravail entree = donnees();
        entree.setId(99);
        entree.setIdCandidature(42);
        ConditionTravail resultat = service.definirConditionsTravail(poste, entree);
        assertNull(resultat.getId());
        assertNull(resultat.getIdCandidature());
        assertSame(poste, resultat.getPoste());
        assertSame(rh, resultat.getRoleProposant());
        assertEquals(4, resultat.getVersion());
    }

    @Test
    void conserveLesConditionsRhEtCreeLaContreProposition() {
        ConditionTravail ancienne = donnees();
        ancienne.setId(7);
        ancienne.setIdCandidature(12);
        Role rh = new Role();
        rh.setCode("RH");
        ancienne.setRoleProposant(rh);
        when(conditions.findByIdForUpdate(7)).thenReturn(Optional.of(ancienne));
        when(conditions.findFirstByIdCandidatureOrderByVersionDesc(12)).thenReturn(Optional.of(ancienne));
        Role candidat = references("CANDIDAT");
        ReponseCondition autre = new ReponseCondition();
        autre.setCode("AUTRE");
        when(reponses.findByCode("AUTRE")).thenReturn(Optional.of(autre));
        ConditionTravail entree = donnees();
        entree.setSalaire(new BigDecimal("1200.00"));
        entree.setIdCandidature(999);
        ConditionTravail resultat = service.modifierConditionsTravail(7, entree);
        assertEquals(new BigDecimal("1000.00"), ancienne.getSalaire());
        assertSame(autre, ancienne.getReponse());
        assertNull(resultat.getId());
        assertNull(resultat.getPoste());
        assertNull(resultat.getReponse());
        assertEquals(12, resultat.getIdCandidature());
        assertEquals(2, resultat.getVersion());
        assertSame(candidat, resultat.getRoleProposant());
        assertEquals(new BigDecimal("1200.00"), resultat.getSalaire());
    }

    @Test
    void refuseLaModificationDesConditionsTypesDuPoste() {
        ConditionTravail ancienne = donnees();
        ancienne.setPoste(new Poste());
        when(conditions.findByIdForUpdate(7)).thenReturn(Optional.of(ancienne));
        assertThrows(IllegalStateException.class, () -> service.modifierConditionsTravail(7, donnees()));
        verify(conditions, never()).save(any());
    }

    @Test
    void refuseUneVersionDepassee() {
        ConditionTravail ancienne = donnees();
        ancienne.setId(7);
        ancienne.setIdCandidature(12);
        ConditionTravail derniere = donnees();
        derniere.setId(8);
        when(conditions.findByIdForUpdate(7)).thenReturn(Optional.of(ancienne));
        when(conditions.findFirstByIdCandidatureOrderByVersionDesc(12)).thenReturn(Optional.of(derniere));
        assertThrows(IllegalStateException.class, () -> service.modifierConditionsTravail(7, donnees()));
        verify(conditions, never()).save(any());
    }

    @Test
    void refuseLesHeuresNullesAvantToutEnregistrement() {
        ConditionTravail entree = donnees();
        entree.setHeuresParSemaine(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> service.modifierConditionsTravail(7, entree));
        verifyNoInteractions(conditions);
    }
}
