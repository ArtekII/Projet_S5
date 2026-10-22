package com.project.service;

import com.project.entity.*;
import com.project.repository.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
        "app.conditions.publication-active=false",
        "spring.datasource.url=jdbc:h2:mem:conditions-workflow;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.jpa.show-sql=false",
        "spring.jpa.show-sql=false", "logging.level.root=WARN"
})
@Transactional
class ConditionTravailWorkflowTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ConditionTravailService service;
    @Autowired ConditionTravailRepository conditions;
    @Autowired PosteRepository postes;
    @Autowired RoleRepository roles;
    @Autowired DecisionRepository decisions;
    @Autowired ReponseConditionRepository reponses;
    @Autowired ContratService contrats;
    @Autowired ContratRepository contratRepository;
    @Autowired TypeContratRepository types;
    @org.springframework.boot.test.mock.mockito.MockBean java.time.Clock clock;
    private Poste poste;

    @BeforeEach
    void preparer() {
        org.mockito.Mockito.when(clock.getZone()).thenReturn(java.time.ZoneId.of("Indian/Antananarivo"));
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-10-21T09:00:00Z"));
        // Les tables non encore mappees en JPA sont celles consultees dans 26.sql.
        jdbc.execute("create table if not exists etape_recrutement (id int primary key, code varchar(20))");
        jdbc.execute("create table if not exists statut_candidature (id int primary key, code varchar(30))");
        jdbc.execute("create table if not exists candidature (id int primary key, id_offre int, id_candidat int, id_etape int, id_statut int)");
        jdbc.execute("alter table candidature add column if not exists cdi_autorise boolean default false");
        jdbc.execute("alter table candidature add column if not exists id_rh_cdi int");
        jdbc.execute("alter table offre_emploi add column if not exists nb_places int default 1");
        jdbc.execute("create table if not exists entretien (id_candidature int primary key, note_totale numeric(5,2))");
        jdbc.execute("""
                create or replace view v_classement as
                select t.*, (t.rang <= t.nb_places) as retenu from (
                    select c.id_offre, c.id as id_candidature, e.note_totale, o.nb_places,
                        rank() over (partition by c.id_offre order by e.note_totale desc) as rang
                    from candidature c
                    join statut_candidature s on s.id = c.id_statut and s.code = 'EN_COURS'
                    join entretien e on e.id_candidature = c.id
                    join offre_emploi o on o.id = c.id_offre
                ) t
                """);
        jdbc.update("insert into etape_recrutement values (1, 'DOSSIER'), (3, 'ENTRETIEN'), (4, 'CONDITIONS'), (5, 'EMBAUCHE')");
        jdbc.update("insert into statut_candidature values (1, 'EN_COURS'), (3, 'REFUSE_PAR_CANDIDAT'), (4, 'EMBAUCHE')");
        for (String code : new String[]{"RH", "CANDIDAT"}) {
            Role role = new Role(); role.setCode(code); role.setLibelle(code); roles.saveAndFlush(role);
        }
        for (String code : new String[]{"EN_ATTENTE", "VALIDEE", "REFUSEE"}) {
            Decision d = new Decision(); d.setCode(code); d.setLibelle(code); decisions.saveAndFlush(d);
        }
        ReponseCondition autre = new ReponseCondition(); autre.setCode("AUTRE"); autre.setLibelle("Modification"); reponses.saveAndFlush(autre);
        ReponseCondition oui = new ReponseCondition(); oui.setCode("OUI"); oui.setLibelle("Accepte"); reponses.saveAndFlush(oui);
        ReponseCondition non = new ReponseCondition(); non.setCode("NON"); non.setLibelle("Refuse"); reponses.saveAndFlush(non);
        for (String code : new String[]{"CE", "CDI"}) {
            TypeContrat type = new TypeContrat(); type.setCode(code); type.setLibelle(code); types.saveAndFlush(type);
        }
        poste = new Poste(); poste.setIntitule("Developpeur"); poste = postes.saveAndFlush(poste);
        jdbc.update("insert into utilisateur (id, email) values (100, 'candidat@example.com')");
        jdbc.update("insert into offre_emploi (id, id_poste, nb_places) values (100, ?, 1)", poste.getId());
        jdbc.update("insert into candidature (id, id_offre, id_candidat, id_etape, id_statut) values (100, 100, 100, 4, 1)");
        jdbc.update("insert into entretien values (100, 50)");
        service.definirConditionsTravail(poste, donnees("1000.00"));
    }

    private ConditionTravail donnees(String salaire) {
        ConditionTravail c = new ConditionTravail();
        c.setSalaire(new BigDecimal(salaire));
        c.setHeuresParSemaine(new BigDecimal("40.0"));
        c.setLieuTravail("Antananarivo");
        return c;
    }

    @Test
    void retenuApresEntretienAccedeUniquementASesConditions() {
        jdbc.update("update candidature set id_etape = 3 where id = 100");
        assertTrue(service.dossiers(999).isEmpty());
        assertEquals(3, jdbc.queryForObject("select id_etape from candidature where id = 100", Integer.class));
        assertEquals(1, service.dossiers(100).size());
        assertEquals(4, jdbc.queryForObject("select id_etape from candidature where id = 100", Integer.class));
    }

    @Test
    void sansEntretienOuAvantTroisiemeEtapeAucunAcces() {
        jdbc.update("delete from entretien where id_candidature = 100");
        assertTrue(service.dossiers(100).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.proposer(100, 100, donnees("1100")));
        assertThrows(IllegalArgumentException.class, () -> service.refuser(100, 100));
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditions(100, 100));
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditionsInitiales(100, 100));
        assertTrue(conditions.findFirstByIdCandidatureOrderByVersionDesc(100).isEmpty());
        jdbc.update("insert into entretien values (100, 50)");
        jdbc.update("update candidature set id_etape = 1 where id = 100");
        assertTrue(service.dossiers(100).isEmpty());
    }

    @Test
    void candidatNonRetenuNeVoitPasLesConditionsMemeSiEtapeForcee() {
        jdbc.update("insert into candidature (id, id_offre, id_candidat, id_etape, id_statut) values (101, 100, 100, 3, 1)");
        jdbc.update("insert into entretien values (101, 60)");
        assertEquals(101, service.dossiers(100).get(0).get("id"));
        assertEquals(1, service.dossiers(100).size());
        assertThrows(IllegalArgumentException.class, () -> service.refuser(100, 100));
        assertThrows(IllegalArgumentException.class, () -> service.proposer(100, 100, donnees("1100")));
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditions(100, 100));
    }


    private ConditionTravail expirer() {
        service.dossiers(100);
        ConditionTravail condition = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        condition.setRepondreAvant(java.time.LocalDateTime.of(2026, 10, 21, 12, 0));
        conditions.saveAndFlush(condition);
        return condition;
    }

    @Test
    void publicationSansVisiteDemarreLesSeptJours() {
        service.publierConditionsDisponibles();
        ConditionTravail condition = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        assertEquals(java.time.LocalDateTime.of(2026, 10, 28, 12, 0), condition.getRepondreAvant());
        service.dossiers(100);
        assertEquals(java.time.LocalDateTime.of(2026, 10, 28, 12, 0), condition.getRepondreAvant());
    }

    @Test
    void autorisationCdiEstAttribueeEtAuditeeAvantLeContrat() {
        contrats.autoriserCdi(100, 42);
        assertEquals(42, jdbc.queryForObject("select id_rh_cdi from candidature where id = 100", Integer.class));
        assertEquals("CDI", contrats.accepterConditions(100, 100).getTypeContrat().getCode());
        assertThrows(IllegalStateException.class, () -> contrats.autoriserCdi(100, 43));
        assertEquals(42, jdbc.queryForObject("select id_rh_cdi from candidature where id = 100", Integer.class));
    }

    @Test
    void acceptationAttribueLeContratEtConfirmeImmediatementLEmbauche() {
        Contrat contrat = contrats.accepterConditions(100, 100);
        assertEquals("CE", contrat.getTypeContrat().getCode());
        assertEquals("FINALISE", contrat.getStatut());
        assertNull(contrat.getDateDebut());
        assertNull(contrat.getDateFin());
        assertEquals(5, jdbc.queryForObject("select id_etape from candidature where id = 100", Integer.class));
        assertEquals(4, jdbc.queryForObject("select id_statut from candidature where id = 100", Integer.class));
        assertEquals(contrat.getId(), contrats.accepterConditions(100, 100).getId());
        assertEquals(1, contratRepository.count());
    }

    @Test
    void expirationBloqueToutesLesReponsesDuCandidat() {
        expirer();
        assertEquals(true, service.dossiers(100).get(0).get("expire"));
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditions(100, 100));
        assertThrows(IllegalStateException.class, () -> service.refuser(100, 100));
        assertThrows(IllegalStateException.class, () -> service.proposer(100, 100, donnees("1200.00")));
        assertEquals(0, contratRepository.count());
    }

    @Test
    void prolongationRouvreLaReponseEtNeRaccourcitPasLeDelai() {
        ConditionTravail condition = expirer();
        service.prolonger(100);
        assertEquals(java.time.LocalDateTime.of(2026, 10, 28, 12, 0), condition.getRepondreAvant());
        service.prolonger(100);
        assertEquals(java.time.LocalDateTime.of(2026, 11, 4, 12, 0), condition.getRepondreAvant());
        assertEquals("CE", contrats.accepterConditions(100, 100).getTypeContrat().getCode());
    }

    @Test
    void decisionRhRedemarreLeDelaiApresExamen() {
        service.proposer(100, 100, donnees("1200.00"));
        ConditionTravail actuelle = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        assertNull(actuelle.getRepondreAvant());
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-11-21T09:00:00Z"));
        service.decider(actuelle.getId(), false);
        assertEquals(java.time.LocalDateTime.of(2026, 11, 28, 12, 0), actuelle.getRepondreAvant());
    }

    @Test
    void expirationBloqueAussiLaRepriseDesConditionsInitiales() {
        ConditionTravail actuelle = contrePropositionRefusee();
        actuelle.setRepondreAvant(java.time.LocalDateTime.of(2026, 10, 21, 12, 0));
        conditions.saveAndFlush(actuelle);
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditionsInitiales(100, 100));
        assertEquals(2, conditions.findByIdCandidatureOrderByVersionDesc(100).size());
    }

    private ConditionTravail contrePropositionRefusee() {
        service.proposer(100, 100, donnees("1200.00"));
        ConditionTravail proposition = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        service.decider(proposition.getId(), false);
        return proposition;
    }

    @Test
    void peutRenegocierApresRefusSansAltererLHistorique() {
        ConditionTravail refusee = contrePropositionRefusee();
        service.proposer(100, 100, donnees("1100.00"));
        ConditionTravail nouvelle = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        assertEquals(3, nouvelle.getVersion());
        assertEquals("EN_ATTENTE", nouvelle.getDecisionRh().getCode());
        assertEquals("REFUSEE", refusee.getDecisionRh().getCode());
        assertNull(refusee.getReponse());
        service.decider(nouvelle.getId(), true);
        assertEquals(new BigDecimal("1100.00"), contrats.accepterConditions(100, 100).getCondition().getSalaire());
    }

    @Test
    void accepteLesConditionsOriginalesMemeSiLeModeleDuPosteChange() {
        ConditionTravail refusee = contrePropositionRefusee();
        service.definirConditionsTravail(poste, donnees("2000.00"));
        Contrat contrat = contrats.accepterConditionsInitiales(100, 100);
        assertEquals(new BigDecimal("1000.00"), contrat.getCondition().getSalaire());
        assertEquals(3, contrat.getCondition().getVersion());
        assertEquals("OUI", contrat.getCondition().getReponse().getCode());
        assertEquals("REFUSEE", refusee.getDecisionRh().getCode());
        assertEquals("AUTRE", conditions.findByIdCandidatureOrderByVersionDesc(100).get(2).getReponse().getCode());
        assertEquals(contrat.getId(), contrats.accepterConditionsInitiales(100, 100).getId());
    }

    @Test
    void peutCloturerApresUneContrePropositionRefusee() {
        contrePropositionRefusee();
        service.refuser(100, 100);
        assertEquals(3, jdbc.queryForObject("select id_statut from candidature where id = 100", Integer.class));
        assertEquals(0, contratRepository.count());
    }

    @Test
    void nePeutAccepterInitialesPourUnAutreCandidat() {
        contrePropositionRefusee();
        assertThrows(IllegalArgumentException.class, () -> contrats.accepterConditionsInitiales(100, 200));
        assertEquals(2, conditions.findByIdCandidatureOrderByVersionDesc(100).size());
    }

    @Test
    void nePeutAccepterInitialesPendantValidationRh() {
        service.proposer(100, 100, donnees("1200.00"));
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditionsInitiales(100, 100));
        assertEquals(0, contratRepository.count());
    }

    @Test
    void nePeutAccepterDirectementUneContrePropositionRefusee() {
        contrePropositionRefusee();
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditions(100, 100));
        assertEquals(0, contratRepository.count());
    }

    @Test
    void refusClotureLaCandidatureSansModifierLeModeleRh() {
        service.refuser(100, 100);
        ConditionTravail condition = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        assertEquals("NON", condition.getReponse().getCode());
        assertEquals("EN_ATTENTE", condition.getDecisionRh().getCode());
        assertNull(conditions.findFirstByPoste_IdOrderByVersionDesc(poste.getId()).orElseThrow().getReponse());
        assertEquals(3, jdbc.queryForObject("select id_statut from candidature where id = 100", Integer.class));
        assertTrue(service.dossiers(100).isEmpty());
        assertEquals(0, contratRepository.count());
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditions(100, 100));
    }

    @Test
    void refusInterditPourUnAutreCandidat() {
        assertThrows(IllegalArgumentException.class, () -> service.refuser(100, 200));
        assertTrue(conditions.findByIdCandidatureOrderByVersionDesc(100).isEmpty());
        assertEquals(1, jdbc.queryForObject("select id_statut from candidature where id = 100", Integer.class));
    }

    @Test
    void refusInterditApresCreationDuContrat() {
        contrats.accepterConditions(100, 100);
        assertThrows(IllegalArgumentException.class, () -> service.refuser(100, 100));
    }

    @Test
    void refusInterditPendantValidationRh() {
        service.proposer(100, 100, donnees("1200.00"));
        assertThrows(IllegalStateException.class, () -> service.refuser(100, 100));
        assertEquals(1, jdbc.queryForObject("select id_statut from candidature where id = 100", Integer.class));
    }

    @Test
    void copieLaBaseDuPosteEtValideUneNegociationIndividuelle() {
        assertEquals(1, service.dossiers(100).size());
        assertTrue(service.dossiers(200).isEmpty());
        service.proposer(100, 100, donnees("1200.00"));
        ConditionTravail proposition = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        assertEquals(2, proposition.getVersion());
        assertEquals("CANDIDAT", proposition.getRoleProposant().getCode());
        assertEquals("EN_ATTENTE", proposition.getDecisionRh().getCode());
        assertEquals(2, conditions.findByIdCandidatureOrderByVersionDesc(100).size());
        assertEquals(new BigDecimal("1000.00"), conditions.findFirstByPoste_IdOrderByVersionDesc(poste.getId()).orElseThrow().getSalaire());
        service.decider(proposition.getId(), true);
        assertEquals("VALIDEE", conditions.findById(proposition.getId()).orElseThrow().getDecisionRh().getCode());
        assertThrows(IllegalStateException.class, () -> service.decider(proposition.getId(), false));
    }

    @Test
    void unCandidatNePeutPasNegocierPourUnAutre() {
        assertThrows(IllegalArgumentException.class, () -> service.proposer(100, 200, donnees("1200.00")));
        assertTrue(conditions.findByIdCandidatureOrderByVersionDesc(100).isEmpty());
    }

    @Test
    void refuseLesChangementsSansModifierLaBaseRh() {
        service.proposer(100, 100, donnees("1200.00"));
        ConditionTravail proposition = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        service.decider(proposition.getId(), false);
        assertEquals("REFUSEE", proposition.getDecisionRh().getCode());
        assertEquals(new BigDecimal("1000.00"), conditions.findFirstByPoste_IdOrderByVersionDesc(poste.getId()).orElseThrow().getSalaire());
    }

    @Test
    void accepterCreeUnCeEtPasseAuContratSansDoublon() {
        Contrat contrat = contrats.accepterConditions(100, 100);
        assertEquals("CE", contrat.getTypeContrat().getCode());
        assertEquals("OUI", contrat.getCondition().getReponse().getCode());
        assertEquals(100, contrat.getCondition().getIdCandidature());
        assertEquals("FINALISE", contrat.getStatut());
        assertEquals(5, jdbc.queryForObject("select id_etape from candidature where id = 100", Integer.class));
        assertEquals(4, jdbc.queryForObject("select id_statut from candidature where id = 100", Integer.class));
        assertEquals(contrat.getId(), contrats.accepterConditions(100, 100).getId());
        assertEquals(1, contratRepository.count());
        assertTrue(contrats.contratsDuCandidat(200).isEmpty());
        assertEquals(1, contrats.contratsDuCandidat(100).size());
    }

    @Test
    void accreditationRhDonneUnCdi() {
        jdbc.update("update candidature set cdi_autorise = true where id = 100");
        assertEquals("CDI", contrats.accepterConditions(100, 100).getTypeContrat().getCode());
    }

    @Test
    void nePeutAccepterLaCandidatureDunAutre() {
        assertThrows(IllegalArgumentException.class, () -> contrats.accepterConditions(100, 200));
        assertEquals(0, contratRepository.count());
    }

    @Test
    void nePeutAccepterDesChangementsEnAttenteRh() {
        service.proposer(100, 100, donnees("1200.00"));
        assertThrows(IllegalStateException.class, () -> contrats.accepterConditions(100, 100));
        assertEquals(0, contratRepository.count());
    }

    @Test
    void peutAccepterLesChangementsValidesParLeRh() {
        service.proposer(100, 100, donnees("1200.00"));
        ConditionTravail derniere = conditions.findFirstByIdCandidatureOrderByVersionDesc(100).orElseThrow();
        service.decider(derniere.getId(), true);
        Contrat contrat = contrats.accepterConditions(100, 100);
        assertEquals(derniere.getId(), contrat.getCondition().getId());
        assertEquals(new BigDecimal("1200.00"), contrat.getCondition().getSalaire());
    }
}
