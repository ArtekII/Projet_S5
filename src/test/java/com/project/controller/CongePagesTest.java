package com.project.controller;

import com.project.service.AuthService;
import com.project.service.CongeService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
        "app.conditions.publication-active=false", "app.pointage.detection-active=false",
        "app.contrats.activation-delai-initial-ms=3600000",
        "app.conges.actualisation-delai-initial-ms=3600000",
        "spring.datasource.url=jdbc:h2:mem:conges-pages;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
@Import(CongePagesTest.Configuration.class)
class CongePagesTest {
    @LocalServerPort int port;
    @MockBean CongeService service;
    @MockBean AuthService auth;

    @BeforeEach
    void donnees() {
        when(service.espaceRh(1)).thenReturn(Map.of(
                "demandes", List.of(Map.ofEntries(
                        Map.entry("id", 5),
                        Map.entry("employeId", 1),
                        Map.entry("email", "emp@example.com"),
                        Map.entry("typeCode", "CP"),
                        Map.entry("typeLibelle", "Congés payés"),
                        Map.entry("dateDebut", LocalDate.of(2026, 11, 9)),
                        Map.entry("dateFin", LocalDate.of(2026, 11, 10)),
                        Map.entry("nbJours", new BigDecimal("2.00")),
                        Map.entry("statut", "EN_ATTENTE"),
                        Map.entry("commentaire", "<script>alert(1)</script>Repos"),
                        Map.entry("soldeDisponible", new BigDecimal("7.50")))),
                "soldes", List.of(Map.ofEntries(
                        Map.entry("id", 3),
                        Map.entry("employeId", 1),
                        Map.entry("email", "emp@example.com"),
                        Map.entry("typeCode", "CP"),
                        Map.entry("typeLibelle", "Congés payés"),
                        Map.entry("periodeDebut", LocalDate.of(2026, 9, 1)),
                        Map.entry("periodeFin", LocalDate.of(2027, 8, 31)),
                        Map.entry("joursAcquis", new BigDecimal("7.50")),
                        Map.entry("joursPris", new BigDecimal("0.00")),
                        Map.entry("joursEnAttente", new BigDecimal("2.00")),
                        Map.entry("soldeReserve", new BigDecimal("5.50"))))));
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
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        get(client, "/__test/session?role=" + role);
        return client;
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void afficheLesDemandesEtLesSoldesRh() throws Exception {
        HttpResponse<String> response = get(client("RH"), "/rh/conges");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Demandes de congés"));
        assertTrue(response.body().contains("Soldes des employés"));
        assertTrue(response.body().contains("emp@example.com"));
        assertTrue(response.body().contains("VALIDER"));
        assertTrue(response.body().contains("REFUSER"));
        assertTrue(response.body().contains("name=\"dossierCsrf\""));
        assertFalse(response.body().contains("<script>alert(1)</script>"));
    }

    @Test
    void protegeLaPageRh() throws Exception {
        assertEquals(302, get(client("CANDIDAT"), "/rh/conges").statusCode());
    }
}
