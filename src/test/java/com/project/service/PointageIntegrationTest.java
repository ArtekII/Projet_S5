package com.project.service;

import com.project.dto.*;
import com.project.entity.*;
import com.project.repository.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
        "app.conditions.publication-active=false", "app.pointage.detection-active=false", "app.contrats.activation-delai-initial-ms=3600000",
        "spring.datasource.url=jdbc:h2:mem:pointages;MODE=PostgreSQL", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class PointageIntegrationTest {
    static final String MOT_DE_PASSE_TEST = new BCryptPasswordEncoder().encode("secret");
    static final LocalDate LUNDI = LocalDate.of(2026, 11, 9);
    @Autowired PointageService service;
    @Autowired AuthService auth;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired RoleRepository roles;
    @Autowired EmployeRepository employes;
    @Autowired PlanningRepository plannings;
    @Autowired PointageRepository pointages;
    @Autowired AlertePointageRepository alertes;
    @Autowired CorrectionPointageRepository corrections;
    @Autowired VariablePaiePointageRepository variables;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockBean Clock clock;
    @LocalServerPort int port;
    Utilisateur rh, titulaire, autre, finance;
    int employeId, posteId;

    @BeforeEach void preparer() {
        when(clock.getZone()).thenReturn(ZoneId.of("Indian/Antananarivo"));
        heure("2026-11-09T08:00");
        rh = utilisateur("RH"); titulaire = utilisateur("CANDIDAT"); autre = utilisateur("CANDIDAT"); finance = utilisateur("FINANCE");
        jdbc.update("insert into poste (intitule) values ('Poste <script>test</script>')"); posteId = dernierId("poste");
        jdbc.update("insert into offre_emploi (id_poste) values (?)", posteId); int offreId = dernierId("offre_emploi");
        jdbc.update("insert into cv_candidat (id_candidat, id_offre, nom, prenom, annee_experience, created_at) values (?, ?, 'Test', 'Employé', 0, current_timestamp)", titulaire.getId(), offreId);
        int cvId = dernierId("cv_candidat");
        Integer decision = jdbc.query("select id from decision where code = 'VALIDEE'", rs -> rs.next() ? rs.getInt(1) : null);
        if (decision == null) { jdbc.update("insert into decision (code, libelle) values ('VALIDEE', 'Validée')"); decision = dernierId("decision"); }
        Integer type = jdbc.query("select id from type_contrat where code = 'CDI'", rs -> rs.next() ? rs.getInt(1) : null);
        if (type == null) { jdbc.update("insert into type_contrat (code, libelle) values ('CDI', 'CDI')"); type = dernierId("type_contrat"); }
        jdbc.update("insert into condition_travail (id_poste, version, id_role_proposant, salaire, heures_par_semaine, lieu_travail, id_decision_rh, created_at) values (?, 1, ?, 1500000, 40, 'Antananarivo', ?, current_timestamp)", posteId, rh.getRole().getId(), decision);
        int conditionId = dernierId("condition_travail");
        jdbc.update("insert into contrat (id_condition, id_type_contrat, date_debut, statut, created_at) values (?, ?, '2026-11-01', 'FINALISE', current_timestamp)", conditionId, type);
        int contratId = dernierId("contrat");
        jdbc.update("insert into employe (id_utilisateur, id_cv_candidat, id_contrat_actuel, date_embauche) values (?, ?, ?, '2026-11-01')", titulaire.getId(), cvId, contratId);
        employeId = jdbc.queryForObject("select id_employe from employe where id_utilisateur = ?", Integer.class, titulaire.getId());
        jdbc.update("update contrat set id_employe = ? where id = ?", employeId, contratId);
    }

    private int dernierId(String table) { return jdbc.queryForObject("select max(id) from " + table, Integer.class); }
    private void heure(String date) { when(clock.instant()).thenReturn(LocalDateTime.parse(date).atZone(ZoneId.of("Indian/Antananarivo")).toInstant()); }
    private Utilisateur utilisateur(String code) {
        Role role = roles.findByCode(code).orElseGet(() -> roles.saveAndFlush(new Role(code, code)));
        Utilisateur utilisateur = new Utilisateur(); utilisateur.setEmail(UUID.randomUUID() + "@example.com");
        utilisateur.setRole(role); utilisateur.setMotDePasse(MOT_DE_PASSE_TEST); return utilisateurs.saveAndFlush(utilisateur);
    }
    private PlanningForm formulaire(LocalDate effet) {
        PlanningForm f = new PlanningForm(); f.setPosteId(posteId); f.setDateEffet(effet); f.setJourSemaine(1);
        f.setHeureDebut(LocalTime.of(8, 0)); f.setHeureFin(LocalTime.of(17, 0));
        f.setPauseDebut(LocalTime.of(12, 0)); f.setPauseFin(LocalTime.of(13, 0)); return f;
    }
    private Planning planning() { return service.definirPlanning(rh.getId(), formulaire(LUNDI)); }
    private String jeton() { return UUID.randomUUID().toString(); }
    private BilanPointage bilan(LocalDate date) {
        return service.rapportRh(rh.getId(), date, date).stream().filter(b -> b.getEmploye().getId() == employeId).findFirst().orElseThrow();
    }
    private MockHttpSession session(Utilisateur utilisateur) {
        MockHttpSession session = new MockHttpSession(); session.setAttribute("userId", utilisateur.getId());
        session.setAttribute("userRole", utilisateur.getRole().getCode()); session.setAttribute("pointageCsrf", "token-test"); return session;
    }

    @Test void valideLesHorairesEtConserveLesVersionsDePlanning() {
        Planning initial = planning();
        assertThrows(IllegalArgumentException.class, () -> planning());
        assertThrows(ResponseStatusException.class, () -> service.definirPlanning(titulaire.getId(), formulaire(LUNDI.plusDays(1))));
        PlanningForm invalide = formulaire(LUNDI.plusDays(1)); invalide.setPauseFin(LocalTime.of(19, 0));
        assertThrows(IllegalArgumentException.class, () -> service.definirPlanning(rh.getId(), invalide));
        invalide.setPauseFin(null);
        assertThrows(IllegalArgumentException.class, () -> service.definirPlanning(rh.getId(), invalide));
        invalide.setPauseDebut(null); invalide.setHeureFin(invalide.getHeureDebut());
        assertThrows(IllegalArgumentException.class, () -> service.definirPlanning(rh.getId(), invalide));
        PlanningForm suivant = formulaire(LUNDI.plusDays(7)); suivant.setHeureDebut(LocalTime.of(9, 0));
        Planning nouveau = service.definirPlanning(rh.getId(), suivant);
        assertNotEquals(initial.getId(), nouveau.getId());
        assertEquals(LocalTime.of(8, 0), plannings.findById(initial.getId()).orElseThrow().getHeureDebut());
        assertEquals(initial.getId(), plannings.findFirstByPoste_IdAndJourSemaineAndDateEffetLessThanEqualOrderByDateEffetDesc(posteId, 1, LUNDI).orElseThrow().getId());
    }

    @Test void connexionDeconnexionAutomatiquesEtReconnexionsCalculees() {
        planning(); MockHttpSession premiere = new MockHttpSession();
        heure("2026-11-09T08:15"); auth.login(premiere, titulaire); auth.login(premiere, titulaire);
        assertEquals(1, pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employeId, LUNDI).size());
        heure("2026-11-09T12:00"); auth.logout(premiere);
        MockHttpSession seconde = new MockHttpSession(); heure("2026-11-09T13:00"); auth.login(seconde, titulaire);
        heure("2026-11-09T18:00"); auth.logout(seconde);
        BilanPointage bilan = bilan(LUNDI);
        assertEquals(8 * 3600 + 45 * 60, bilan.getTravailSecondes()); assertEquals(15 * 60, bilan.getRetardSecondes());
        assertEquals(45 * 60, bilan.getSupplementairesSecondes()); assertFalse(bilan.isEnCours());
        assertEquals(2, bilan.getSessions().size());
        auth.login(new MockHttpSession(), autre);
        assertTrue(employes.findByUtilisateurId(autre.getId()).isEmpty());
    }

    @Test void tempsProvisoireNotificationsDedoublonneesEtConfidentialite() {
        planning(); heure("2026-11-09T08:10"); service.connecter(titulaire.getId(), jeton());
        heure("2026-11-09T10:00"); BilanPointage jour = bilan(LUNDI);
        assertTrue(jour.isEnCours()); assertEquals(110 * 60, jour.getTravailSecondes());
        service.detecterAnomalies(); service.detecterAnomalies();
        assertEquals(1, alertes.findByEmploye_IdAndDate(employeId, LUNDI).stream().filter(a -> a.getCode().equals("RETARD")).count());
        var propre = service.espaceUtilisateur(titulaire.getId()); assertEquals(employeId, ((Employe) propre.get("employePointage")).getId());
        assertNull(service.espaceUtilisateur(autre.getId()).get("employePointage"));
        assertThrows(ResponseStatusException.class, () -> service.rapportRh(titulaire.getId(), LUNDI, LUNDI));
        assertThrows(IllegalArgumentException.class, () -> service.transmettre(rh.getId(), employeId, LUNDI));
    }

    @Test void gereMinuitEtConserveLaVersionPriseALaPremiereConnexion() {
        PlanningForm nuit = formulaire(LUNDI.minusDays(1)); nuit.setHeureDebut(LocalTime.of(22, 0)); nuit.setHeureFin(LocalTime.of(6, 0));
        nuit.setPauseDebut(LocalTime.of(23, 45)); nuit.setPauseFin(LocalTime.of(0, 15)); Planning ancien = service.definirPlanning(rh.getId(), nuit);
        heure("2026-11-10T01:00"); String jeton = jeton(); service.connecter(titulaire.getId(), jeton);
        assertEquals(LUNDI, pointages.findByJeton(jeton).orElseThrow().getDate());
        PlanningForm revision = formulaire(LUNDI); revision.setHeureDebut(LocalTime.of(9, 0)); service.definirPlanning(rh.getId(), revision);
        heure("2026-11-10T02:00"); String autreSession = jeton(); service.connecter(titulaire.getId(), autreSession);
        assertEquals(LUNDI, pointages.findByJeton(autreSession).orElseThrow().getDate());
        heure("2026-11-10T06:00"); service.deconnecter(jeton);
        service.deconnecter(autreSession);
        assertEquals(ancien.getId(), bilan(LUNDI).getPlanning().getId());
        assertEquals(5 * 3600, bilan(LUNDI).getTravailSecondes());
    }

    @Test void connexionsConcurrentesIdempotentesEtJourDeRepos() throws Exception {
        PlanningForm repos = formulaire(LUNDI); repos.setJourTravaille(false);
        Planning planning = service.definirPlanning(rh.getId(), repos);
        assertFalse(planning.isJourTravaille()); assertNull(planning.getHeureDebut());
        String jeton = jeton();
        try (var executeur = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var premier = executeur.submit(() -> service.connecter(titulaire.getId(), jeton));
            var second = executeur.submit(() -> service.connecter(titulaire.getId(), jeton));
            premier.get(15, java.util.concurrent.TimeUnit.SECONDS); second.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(1, pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employeId, LUNDI).size());
        heure("2026-11-09T10:00"); service.deconnecter(jeton);
        assertEquals(2 * 3600, bilan(LUNDI).getSupplementairesSecondes());
        assertTrue(bilan(LUNDI).getAnomalies().contains(AnomaliePointage.HORS_PLANNING));
        heure("2026-11-09T11:00"); service.deconnecter(jeton);
        assertEquals(LUNDI.atTime(10, 0), pointages.findByJeton(jeton).orElseThrow().getDeconnexion());
    }

    @Test void expirationRegularisationAuditEtTransmissionImmuable() {
        planning(); String jeton = jeton(); service.connecter(titulaire.getId(), jeton);
        service.expirerSession(jeton); Pointage pointage = pointages.findByJeton(jeton).orElseThrow();
        assertNull(pointage.getDeconnexion());
        assertTrue(bilan(LUNDI).getAnomalies().contains(AnomaliePointage.SORTIE_MANQUANTE));
        heure("2026-11-10T08:00");
        assertThrows(IllegalArgumentException.class, () -> service.transmettre(rh.getId(), employeId, LUNDI));
        assertThrows(ResponseStatusException.class, () -> service.corriger(titulaire.getId(), pointage.getId(), pointage.getRevision(), LUNDI.atTime(8, 0), LUNDI.atTime(17, 0), "Oubli"));
        service.corriger(rh.getId(), pointage.getId(), pointage.getRevision(), LUNDI.atTime(8, 0), LUNDI.atTime(17, 0), "Oubli de déconnexion confirmé");
        assertThrows(IllegalArgumentException.class, () -> service.corriger(rh.getId(), pointage.getId(), pointage.getRevision(), LUNDI.atTime(8, 0), LUNDI.atTime(17, 0), "Version périmée"));
        var corrige = pointages.findByJeton(jeton).orElseThrow();
        assertNull(corrige.getDeconnexion()); assertEquals(LUNDI.atTime(17, 0), corrige.getSortieEffective());
        assertEquals(1, corrections.findByPointage_Employe_IdAndPointage_DateOrderByIdDesc(employeId, LUNDI).size());
        assertNotNull(alertes.findByEmploye_IdAndDate(employeId, LUNDI).stream().filter(a -> a.getCode().equals("SORTIE_MANQUANTE")).findFirst().orElseThrow().getResolueLe());
        VariablePaiePointage variable = service.transmettre(rh.getId(), employeId, LUNDI);
        assertEquals(8 * 3600, variable.getTravailSecondes()); assertEquals(rh.getId(), variable.getRh().getId());
        assertEquals(variable.getId(), service.transmettre(rh.getId(), employeId, LUNDI).getId());
        assertEquals(1, service.variablesFinance(finance.getId(), LUNDI, LUNDI).stream().filter(v -> v.getEmploye().getId() == employeId).count());
        assertThrows(IllegalArgumentException.class, () -> service.corriger(rh.getId(), corrige.getId(), corrige.getRevision(), LUNDI.atTime(8, 0), LUNDI.atTime(16, 0), "Après transmission"));
        assertThrows(ResponseStatusException.class, () -> service.variablesFinance(titulaire.getId(), LUNDI, LUNDI));
    }

    @Test void detecteAbsencesEtAnciennesSortiesManquantesSansInventerDesHeures() {
        planning(); heure("2026-11-09T18:00"); service.detecterAnomalies();
        assertTrue(bilan(LUNDI).getAnomalies().contains(AnomaliePointage.ABSENCE));
        heure("2026-11-09T08:00"); service.connecter(titulaire.getId(), jeton());
        heure("2026-11-23T08:00"); service.detecterAnomalies();
        assertTrue(alertes.findByEmploye_IdAndDate(employeId, LUNDI).stream().anyMatch(a -> a.getCode().equals("SORTIE_MANQUANTE") && a.getResolueLe() == null));
        assertEquals(0, bilan(LUNDI).getTravailSecondes());
    }

    @Test void protectionsHttpEtExportFinance() throws Exception {
        mvc.perform(get("/rh/plannings")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/rh/pointages").session(session(titulaire))).andExpect(status().is3xxRedirection());
        mvc.perform(post("/rh/plannings").session(session(rh))).andExpect(status().isForbidden());
        mvc.perform(post("/rh/plannings").session(session(rh)).param("pointageCsrf", "token-test")
                .param("posteId", "" + posteId).param("jourSemaine", "1").param("dateEffet", "2026-11-09")
                .param("jourTravaille", "true").param("heureDebut", "08:00").param("heureFin", "17:00")
                .param("pauseDebut", "12:00").param("pauseFin", "13:00"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rh/plannings"));
        mvc.perform(post("/rh/plannings").session(session(rh)).param("pointageCsrf", "token-test")
                .param("posteId", "" + posteId).param("jourSemaine", "7").param("dateEffet", "2026-11-09")
                .param("_jourTravaille", "on")).andExpect(status().is3xxRedirection());
        assertFalse(plannings.findFirstByPoste_IdAndJourSemaineAndDateEffetLessThanEqualOrderByDateEffetDesc(posteId, 7, LUNDI.plusDays(6)).orElseThrow().isJourTravaille());
        String jeton = jeton(); service.connecter(titulaire.getId(), jeton); heure("2026-11-09T17:00"); service.deconnecter(jeton);
        heure("2026-11-10T08:00");
        mvc.perform(post("/rh/pointages/transmettre").session(session(rh)).param("pointageCsrf", "token-test")
                .param("employeId", "" + employeId).param("date", "2026-11-09")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/finance/pointages.csv").session(session(finance)).param("du", "2026-11-09").param("au", "2026-11-09"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(employeId + ";2026-11-09;28800;28800;0;0;0;")));
        mvc.perform(get("/finance/pointages.csv").session(session(titulaire)).param("du", "2026-11-09").param("au", "2026-11-09"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/finance/pointages.csv").session(session(finance)).param("du", "2026-10-01").param("au", "2026-11-09"))
                .andExpect(status().isBadRequest());
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private HttpClient client(Utilisateur utilisateur) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        var response = client.send(HttpRequest.newBuilder(uri("/login")).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=" + URLEncoder.encode(utilisateur.getEmail(), StandardCharsets.UTF_8) + "&password=secret")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, response.statusCode(), response.body()); return client;
    }
    private String page(HttpClient client, String path) throws Exception {
        var response = client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body()); return response.body();
    }

    @Test void rendLesPagesJspEtEnregistreLesVraiesConnexionsHttp() throws Exception {
        planning();
        HttpClient clientEmploye = client(titulaire);
        String employe = page(clientEmploye, "/espaceEmploye?onglet=pointages");
        assertTrue(employe.contains("Planning de la semaine")); assertTrue(employe.contains("En cours · provisoire"));
        assertTrue(employe.contains("&lt;script&gt;")); assertFalse(employe.contains("<script>test</script>"));
        HttpClient clientRh = client(rh);
        assertTrue(page(clientRh, "/rh/plannings").contains("Définir un horaire"));
        assertTrue(page(clientRh, "/rh/pointages?du=2026-11-09&au=2026-11-09").contains("Régulariser ce pointage"));
        heure("2026-11-09T17:00");
        var logout = clientEmploye.send(HttpRequest.newBuilder(uri("/logout")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, logout.statusCode());
        assertNotNull(pointages.findByEmploye_IdAndDateOrderByConnexionAscIdAsc(employeId, LUNDI).getFirst().getDeconnexion());
        heure("2026-11-10T08:00"); service.transmettre(rh.getId(), employeId, LUNDI);
        assertTrue(page(client(finance), "/finance/pointages?du=2026-11-09&au=2026-11-09").contains("Exporter les variables en CSV"));
    }
}
