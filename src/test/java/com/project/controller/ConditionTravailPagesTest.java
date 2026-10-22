package com.project.controller;

import com.project.entity.*;
import com.project.service.AuthService;
import com.project.service.ConditionTravailService;
import com.project.service.ContratService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
        "app.conditions.publication-active=false",
        "spring.datasource.url=jdbc:h2:mem:conditions-pages;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.jpa.show-sql=false",
        "spring.jpa.show-sql=false"
})
@Import(ConditionTravailPagesTest.Configuration.class)
class ConditionTravailPagesTest {
    @LocalServerPort int port;
    @MockBean ConditionTravailService service;
    @MockBean AuthService auth;
    @MockBean ContratService contrats;

    @BeforeEach
    void reponsesDisponibles() {
        when(service.reponsesConditions()).thenReturn(java.util.stream.Stream.of("OUI", "NON", "AUTRE")
                .map(code -> {
                    ReponseCondition reponse = new ReponseCondition();
                    reponse.setCode(code);
                    reponse.setLibelle(code);
                    return reponse;
                }).toList());
    }

    @TestConfiguration
    static class Configuration {
        @Bean SessionController sessionController() { return new SessionController(); }
    }

    @Controller
    static class SessionController {
        @GetMapping("/__test/session")
        @ResponseBody
        String session(@RequestParam String role, HttpSession session) {
            session.setAttribute("userId", 1);
            session.setAttribute("userRole", role);
            session.setAttribute("userEmail", "test@example.com");
            return "ok";
        }
    }

    private HttpClient client(String role) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        get(client, "/__test/session?role=" + role);
        return client;
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private ConditionTravail condition(String roleCode) {
        ConditionTravail c = new ConditionTravail();
        c.setId(10);
        c.setSalaire(new BigDecimal("1500000.00"));
        c.setHeuresParSemaine(new BigDecimal("40.0"));
        c.setLieuTravail("Antananarivo <script>alert(1)</script>");
        Role role = new Role();
        role.setCode(roleCode);
        role.setLibelle(roleCode);
        c.setRoleProposant(role);
        Decision decision = new Decision();
        decision.setCode("EN_ATTENTE");
        decision.setLibelle("En attente");
        c.setDecisionRh(decision);
        c.setCreatedAt(java.time.LocalDateTime.now());
        return c;
    }

    @Test
    void afficheLeFormulaireRhEtLaComparaisonAvecLesActions() throws Exception {
        ConditionTravail initiale = condition("RH");
        ConditionTravail actuelle = condition("CANDIDAT");
        when(service.postes()).thenReturn(List.of(Map.of("id", 2, "intitule", "Développeur", "conditions", initiale)));
        when(service.dossiers(null)).thenReturn(List.of(Map.of("id", 3, "intitule", "Développeur", "email", "candidat@example.com",
                "actuelle", actuelle, "initiale", initiale, "historique", List.of(actuelle, initiale))));
        HttpResponse<String> response = get(client("RH"), "/rh/conditions");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Valider les changements"));
        assertTrue(response.body().contains("/rh/conditions/poste/2"));
        assertFalse(response.body().contains("<script>alert(1)</script>"));
    }

    @Test
    void afficheLeFormulaireCandidatEtProtegeLesPagesRh() throws Exception {
        Utilisateur candidat = new Utilisateur();
        candidat.setId(1);
        when(auth.currentUser(any())).thenReturn(Optional.of(candidat));
        ConditionTravail initiale = condition("RH");
        when(service.dossiers(1)).thenReturn(List.of(Map.of("id", 3, "intitule", "Développeur", "actuelle", initiale,
                "initiale", initiale, "historique", List.of(initiale))));
        HttpClient client = client("CANDIDAT");
        HttpResponse<String> response = get(client, "/candidat/conditions");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Envoyer ma proposition au RH"));
        assertTrue(response.body().contains("/candidat/conditions/candidature/3/proposer"));
        assertTrue(response.body().contains("data-reponse=\"OUI\""));
        assertTrue(response.body().contains("data-reponse=\"NON\""));
        assertTrue(response.body().contains("data-reponse=\"AUTRE\""));
        assertTrue(response.body().contains("data-modification-fields"));
        assertEquals(302, get(client, "/rh/conditions").statusCode());
    }

    @Test
    void afficheLesEtatsVides() throws Exception {
        when(service.postes()).thenReturn(List.of());
        when(service.dossiers(null)).thenReturn(List.of());
        HttpResponse<String> response = get(client("RH"), "/rh/conditions");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Aucun poste actif"));
        assertTrue(response.body().contains("Aucune candidature"));
    }

    @Test
    void afficheUnApercuSansCandidatureReelle() throws Exception {
        HttpResponse<String> response = get(client("CANDIDAT"), "/candidat/conditions/apercu");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Design UI/UX"));
        assertTrue(response.body().contains("data-apercu=\"true\""));
        assertTrue(response.body().contains("data-reponse=\"AUTRE\""));
        assertTrue(response.body().contains("data-modifications hidden"));
        assertTrue(response.body().contains("data-modification-fields disabled"));
        assertFalse(response.body().contains("/candidature/0/proposer"));
    }

    @Test
    void afficheLeSuiviCandidatApresSoumission() throws Exception {
        Utilisateur candidat = new Utilisateur();
        candidat.setId(1);
        when(auth.currentUser(any())).thenReturn(Optional.of(candidat));
        ConditionTravail initiale = condition("RH");
        ConditionTravail actuelle = condition("CANDIDAT");
        HttpClient client = client("CANDIDAT");
        String[][] etats = {{"EN_ATTENTE", "Votre proposition est en attente de validation"},
                {"VALIDEE", "Vos changements ont été validés"}, {"REFUSEE", "Vos changements ont été refusés"}};
        for (String[] etat : etats) {
            actuelle.getDecisionRh().setCode(etat[0]);
            when(service.dossiers(1)).thenReturn(List.of(Map.of("id", 3, "intitule", "Développeur", "actuelle", actuelle,
                    "initiale", initiale, "historique", List.of(actuelle, initiale))));
            HttpResponse<String> response = get(client, "/candidat/conditions");
            assertEquals(200, response.statusCode(), response.body());
            assertTrue(response.body().contains(etat[1]));
            assertEquals("REFUSEE".equals(etat[0]), response.body().contains("Envoyer ma proposition au RH"));
            if ("REFUSEE".equals(etat[0])) {
                assertTrue(response.body().contains("/candidat/conditions/candidature/3/accepter-initiales"));
                assertTrue(response.body().contains("clôturer ma candidature"));
            }
        }
    }

    @Test
    void afficheLesContratsDuCandidat() throws Exception {
        Utilisateur candidat = new Utilisateur(); candidat.setId(1);
        when(auth.currentUser(any())).thenReturn(Optional.of(candidat));
        Contrat contrat = new Contrat(); contrat.setId(1); contrat.setIdCandidature(3);
        TypeContrat type = new TypeContrat(); type.setCode("CDI"); type.setLibelle("Contrat a duree indeterminee");
        contrat.setTypeContrat(type); contrat.setCondition(condition("RH"));
        when(contrats.contratsDuCandidat(1)).thenReturn(List.of(contrat));
        HttpResponse<String> response = get(client("CANDIDAT"), "/candidat/conditions");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("CDI"));
        assertTrue(response.body().contains("Vous avez maintenant un CDI"));
        assertFalse(response.body().contains("<script>alert(1)</script>"));
    }
    @Test
    void afficheLeDelaiExpireSansFormulaireDeReponse() throws Exception {
        Utilisateur candidat = new Utilisateur(); candidat.setId(1);
        when(auth.currentUser(any())).thenReturn(Optional.of(candidat));
        ConditionTravail initiale = condition("RH");
        initiale.setRepondreAvant(java.time.LocalDateTime.of(2026, 10, 28, 12, 0));
        when(service.dossiers(1)).thenReturn(List.of(Map.of("id", 3, "intitule", "Developpeur",
                "actuelle", initiale, "initiale", initiale, "historique", List.of(initiale), "expire", true)));
        HttpResponse<String> response = get(client("CANDIDAT"), "/candidat/conditions");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Le délai de réponse est expiré"));
        assertFalse(response.body().contains("/candidature/3/accepter"));
        assertFalse(response.body().contains("/candidature/3/proposer"));
    }

    @Test
    void afficheLeTypeAttribueSansEtapeSupplementaire() throws Exception {
        Utilisateur candidat = new Utilisateur(); candidat.setId(1);
        when(auth.currentUser(any())).thenReturn(Optional.of(candidat));
        Contrat contrat = new Contrat(); contrat.setId(1); contrat.setIdCandidature(3);
        TypeContrat type = new TypeContrat(); type.setCode("CE"); type.setLibelle("Contrat d'essai");
        contrat.setTypeContrat(type); contrat.setCondition(condition("RH"));
        when(contrats.tousLesContrats()).thenReturn(List.of(contrat));
        HttpResponse<String> preparation = get(client("RH"), "/rh/contrats");
        assertEquals(200, preparation.statusCode(), preparation.body());
        assertFalse(preparation.body().contains("name=\"dateDebut\""));
        assertFalse(preparation.body().contains("name=\"dateFin\""));
        contrat.setDateDebut(java.time.LocalDate.of(2026, 10, 22));
        contrat.setDateFin(java.time.LocalDate.of(2027, 1, 22));
        contrat.setStatut("FINALISE");
        when(contrats.contratsDuCandidat(1)).thenReturn(List.of(contrat));
        HttpResponse<String> consultation = get(client("CANDIDAT"), "/candidat/conditions");
        assertEquals(200, consultation.statusCode(), consultation.body());
        assertTrue(consultation.body().contains("CE"));
        assertFalse(consultation.body().contains("Le RH prépare"));
        assertFalse(consultation.body().contains("<form"));
        assertEquals(302, get(client("CANDIDAT"), "/rh/contrats").statusCode());
    }

    @Test
    void routesRhInterditesAuCandidat() throws Exception {
        HttpClient client = client("CANDIDAT");
        for (String path : List.of("/rh/candidatures/3/autoriser-cdi", "/rh/conditions/candidature/3/prolonger")) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(302, response.statusCode());
        }
        org.mockito.Mockito.verify(contrats, org.mockito.Mockito.never()).autoriserCdi(any(), any());
        org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).prolonger(any());
    }

    @Test
    void accreditationSurUnePageRhDediee() throws Exception {
        Utilisateur rh = new Utilisateur(); rh.setId(1);
        when(auth.currentUser(any())).thenReturn(Optional.of(rh));
        when(service.dossiers(null)).thenReturn(List.of(
                Map.of("id", 3, "intitule", "Developpeur", "email", "alice@example.com", "cdi_autorise", false),
                Map.of("id", 4, "intitule", "Developpeur", "email", "bob@example.com", "cdi_autorise", true)));
        HttpClient client = client("RH");
        HttpResponse<String> page = get(client, "/rh/accreditations");
        assertEquals(200, page.statusCode(), page.body());
        assertTrue(page.body().contains("/rh/candidatures/3/autoriser-cdi"));
        assertFalse(page.body().contains("/rh/candidatures/4/autoriser-cdi"));
        assertTrue(page.body().contains("Accréditation CDI accordée"));
        assertFalse(get(client, "/rh/conditions").body().contains("/autoriser-cdi"));
        HttpResponse<String> action = client.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/rh/candidatures/3/autoriser-cdi"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, action.statusCode());
        assertTrue(action.headers().firstValue("location").orElseThrow().endsWith("/rh/accreditations"));
        org.mockito.Mockito.verify(contrats).autoriserCdi(3, 1);
        assertEquals(302, get(client("CANDIDAT"), "/rh/accreditations").statusCode());
    }

    @Test
    void acceptationEtAncienLienContratsReviennentAuxConditions() throws Exception {
        Utilisateur candidat = new Utilisateur(); candidat.setId(1);
        when(auth.currentUser(any())).thenReturn(Optional.of(candidat));
        Contrat contrat = new Contrat();
        TypeContrat type = new TypeContrat(); type.setCode("CE"); contrat.setTypeContrat(type);
        when(contrats.accepterConditions(3, 1)).thenReturn(contrat);
        when(contrats.accepterConditionsInitiales(3, 1)).thenReturn(contrat);
        HttpClient client = client("CANDIDAT");
        for (String action : List.of("accepter", "accepter-initiales")) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(
                    "http://localhost:" + port + "/candidat/conditions/candidature/3/" + action))
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(302, response.statusCode());
            assertTrue(response.headers().firstValue("location").orElseThrow().endsWith("/candidat/conditions"));
        }
        HttpResponse<String> ancienLien = get(client, "/candidat/contrats");
        assertEquals(302, ancienLien.statusCode());
        assertTrue(ancienLien.headers().firstValue("location").orElseThrow().endsWith("/candidat/conditions"));
        assertFalse(get(client, "/candidat/conditions").body().contains("href=\"/candidat/contrats\""));
    }

}
