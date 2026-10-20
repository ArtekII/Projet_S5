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
        "spring.datasource.url=jdbc:h2:mem:conditions-workflow;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
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
    private Poste poste;

    @BeforeEach
    void preparer() {
        // Les tables non encore mappees en JPA sont celles consultees dans 26.sql.
        jdbc.execute("create table if not exists etape_recrutement (id int primary key, code varchar(20))");
        jdbc.execute("create table if not exists statut_candidature (id int primary key, code varchar(30))");
        jdbc.execute("create table if not exists candidature (id int primary key, id_offre int, id_candidat int, id_etape int, id_statut int)");
        jdbc.execute("alter table candidature add column if not exists cdi_autorise boolean default false");
        jdbc.update("insert into etape_recrutement values (4, 'CONDITIONS'), (5, 'EMBAUCHE')");
        jdbc.update("insert into statut_candidature values (1, 'EN_COURS')");
        for (String code : new String[]{"RH", "CANDIDAT"}) {
            Role role = new Role(); role.setCode(code); role.setLibelle(code); roles.saveAndFlush(role);
        }
        for (String code : new String[]{"EN_ATTENTE", "VALIDEE", "REFUSEE"}) {
            Decision d = new Decision(); d.setCode(code); d.setLibelle(code); decisions.saveAndFlush(d);
        }
        ReponseCondition autre = new ReponseCondition(); autre.setCode("AUTRE"); autre.setLibelle("Modification"); reponses.saveAndFlush(autre);
        ReponseCondition oui = new ReponseCondition(); oui.setCode("OUI"); oui.setLibelle("Accepte"); reponses.saveAndFlush(oui);
        for (String code : new String[]{"CE", "CDI"}) {
            TypeContrat type = new TypeContrat(); type.setCode(code); type.setLibelle(code); types.saveAndFlush(type);
        }
        poste = new Poste(); poste.setIntitule("Developpeur"); poste = postes.saveAndFlush(poste);
        jdbc.update("insert into utilisateur (id, email) values (100, 'candidat@example.com')");
        jdbc.update("insert into offre_emploi (id, id_poste) values (100, ?)", poste.getId());
        jdbc.update("insert into candidature (id, id_offre, id_candidat, id_etape, id_statut) values (100, 100, 100, 4, 1)");
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
        assertEquals(5, jdbc.queryForObject("select id_etape from candidature where id = 100", Integer.class));
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
