package com.project.service;

import com.project.entity.Conge;
import com.project.entity.Employe;
import com.project.entity.Role;
import com.project.entity.TypeConge;
import com.project.entity.Utilisateur;
import com.project.repository.CongeRepository;
import com.project.repository.EmployeRepository;
import com.project.repository.RoleRepository;
import com.project.repository.TypeCongeRepository;
import com.project.repository.UtilisateurRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
        "app.conditions.publication-active=false", "app.pointage.detection-active=false",
        "app.contrats.activation-delai-initial-ms=3600000",
        "app.conges.actualisation-delai-initial-ms=3600000",
        "spring.datasource.url=jdbc:h2:mem:conges;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
class CongeWorkflowTest {
    static final LocalDate LUNDI = LocalDate.of(2026, 11, 9);

    @Autowired CongeService service;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired RoleRepository roles;
    @Autowired EmployeRepository employes;
    @Autowired TypeCongeRepository types;
    @Autowired CongeRepository conges;
    @Autowired JdbcTemplate jdbc;
    @MockBean Clock clock;

    Utilisateur rh;
    Utilisateur titulaire;
    Employe employe;
    TypeConge cp;

    @BeforeEach
    void preparer() {
        heure("2026-11-09T09:00");
        jdbc.update("delete from historique_conge");
        jdbc.update("delete from conge");
        jdbc.update("delete from solde_conge");
        jdbc.update("delete from employe");
        jdbc.update("delete from utilisateur");

        rh = utilisateur("RH");
        titulaire = utilisateur("CANDIDAT");
        employe = new Employe();
        employe.setUtilisateurId(titulaire.getId());
        employe.setCvCandidatId(1);
        employe.setContratId(1);
        employe.setDateEmbauche(LocalDate.of(2026, 9, 1));
        employe = employes.saveAndFlush(employe);
        cp = types.findByCode("CP").orElseThrow();
    }

    @Test
    void calculeLeSoldePuisValideUneDemandeEtLeDecompte() {
        Map<String, Object> vue = service.espaceEmploye(titulaire.getId());
        assertEquals(employe.getId(), ((Employe) vue.get("employeConge")).getId());
        Map<String, Object> solde = solde(vue, "CP");
        assertEquals(new BigDecimal("7.50"), solde.get("joursAcquis"));
        assertEquals(new BigDecimal("7.50"), solde.get("soldeDisponible"));

        service.demander(titulaire.getId(), cp.getId(), LUNDI, LUNDI.plusDays(1), "Repos");
        Conge demande = conges.findAll().getFirst();
        assertEquals(Conge.STATUT_EN_ATTENTE, demande.getStatut());
        assertEquals(new BigDecimal("2.00"), demande.getNbJours());

        service.valider(rh.getId(), demande.getId(), "Accordé");
        Conge validee = conges.findById(demande.getId()).orElseThrow();
        assertEquals(Conge.STATUT_VALIDE, validee.getStatut());
        assertEquals(new BigDecimal("2.00"), jours("jours_pris"));
        assertEquals(new BigDecimal("5.50"), jours("jours_acquis - jours_pris"));
        assertEquals(2, jdbc.queryForObject("select count(*) from historique_conge where id_conge = ?", Integer.class, demande.getId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from historique_conge where id_conge = ? and action = 'VALIDATION'", Integer.class, demande.getId()));
    }

    @Test
    void refuseUneDemandeQuiDepasseLeSoldeEtLesChevauchements() {
        assertThrows(IllegalArgumentException.class,
                () -> service.demander(titulaire.getId(), cp.getId(), LUNDI, LUNDI.plusDays(10), null));

        service.demander(titulaire.getId(), cp.getId(), LUNDI, LUNDI.plusDays(1), null);
        assertThrows(IllegalArgumentException.class,
                () -> service.demander(titulaire.getId(), cp.getId(), LUNDI.plusDays(1), LUNDI.plusDays(3), null));
    }

    @Test
    void refusEtAnnulationNeConsommentPasLeSolde() {
        service.demander(titulaire.getId(), cp.getId(), LUNDI, LUNDI, null);
        Conge premier = conges.findAll().getFirst();
        service.refuser(rh.getId(), premier.getId(), "Période chargée");
        assertEquals(Conge.STATUT_REFUSE, conges.findById(premier.getId()).orElseThrow().getStatut());
        assertEquals(new BigDecimal("0.00"), jours("jours_pris"));

        service.demander(titulaire.getId(), cp.getId(), LUNDI.plusDays(1), LUNDI.plusDays(1), null);
        Conge second = conges.findAll().stream()
                .filter(c -> c.getDateDebut().equals(LUNDI.plusDays(1))).findFirst().orElseThrow();
        service.annuler(titulaire.getId(), second.getId());
        assertEquals(Conge.STATUT_ANNULE, conges.findById(second.getId()).orElseThrow().getStatut());
        assertEquals(new BigDecimal("0.00"), jours("jours_pris"));
    }

    @Test
    void seulLeRhPeutValiderUneDemande() {
        service.demander(titulaire.getId(), cp.getId(), LUNDI, LUNDI, null);
        Conge demande = conges.findAll().getFirst();
        assertThrows(ResponseStatusException.class,
                () -> service.valider(titulaire.getId(), demande.getId(), null));
        assertThrows(ResponseStatusException.class,
                () -> service.espaceRh(titulaire.getId()));
    }

    private Map<String, Object> solde(Map<String, Object> vue, String code) {
        List<Map<String, Object>> soldes = (List<Map<String, Object>>) vue.get("soldesConges");
        return soldes.stream().filter(s -> code.equals(s.get("typeCode"))).findFirst().orElseThrow();
    }

    private BigDecimal jours(String expression) {
        BigDecimal valeur = jdbc.queryForObject(
                "select " + expression + " from solde_conge where id_employe = ? and id_type_conge = ?",
                BigDecimal.class, employe.getId(), cp.getId());
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    private void heure(String valeur) {
        when(clock.getZone()).thenReturn(ZoneId.of("Indian/Antananarivo"));
        when(clock.instant()).thenReturn(LocalDateTime.parse(valeur)
                .atZone(ZoneId.of("Indian/Antananarivo")).toInstant());
    }

    private Utilisateur utilisateur(String code) {
        Role role = roles.findByCode(code).orElseGet(() -> roles.saveAndFlush(new Role(code, code)));
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(UUID.randomUUID() + "@example.com");
        utilisateur.setRole(role);
        utilisateur.setMotDePasse("secret");
        return utilisateurs.saveAndFlush(utilisateur);
    }
}
