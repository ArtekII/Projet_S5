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
        "spring.datasource.url=jdbc:h2:mem:conditions-pages;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
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
            assertFalse(response.body().contains("Envoyer ma proposition au RH"));
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
        HttpResponse<String> response = get(client("CANDIDAT"), "/candidat/contrats");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("CDI"));
        assertTrue(response.body().contains("Conditions acceptées"));
        assertFalse(response.body().contains("<script>alert(1)</script>"));
    }
}
