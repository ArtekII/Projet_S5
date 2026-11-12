package com.project.controller;

import com.project.entity.*;
import com.project.service.*;
import com.project.repository.PointageRepository;
import com.project.repository.EmployeRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
    "app.conditions.publication-active=false", "spring.sql.init.mode=never",
    "spring.datasource.url=jdbc:h2:mem:ui-pages;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@Import(UserInterfacePagesTest.Configuration.class)
class UserInterfacePagesTest {
    @LocalServerPort int port;
    @MockBean AuthService auth;
    @MockBean OffreService offres;
    @MockBean CandidatureService candidatures;
    @MockBean QuestionnaireService questionnaires;
    @MockBean ReponseCandidatService reponses;
    @MockBean PointageRepository pointages;
    @MockBean EmployeRepository employes;
    @Autowired JdbcTemplate jdbc;
    @Autowired DashboardRhService dashboard;
    @Autowired Clock clock;

    @TestConfiguration
    static class Configuration {
        @Bean SessionController uiSessionController() { return new SessionController(); }
    }

    @Controller
    static class SessionController {
        @GetMapping("/__ui/session") @ResponseBody
        String session(@RequestParam String role, @RequestParam(defaultValue="false") boolean pointage, HttpSession session) {
            session.setAttribute("userId", 1);
            session.setAttribute("userRole", role);
            session.setAttribute("userEmail", "test@example.com");
            if (pointage) session.setAttribute("pointageToken", "ui-pointage");
            return "ok";
        }
    }

    private OffreEmploi offre;

    @BeforeEach
    void donnees() {
        Departement departement = new Departement(); departement.setNom("Informatique");
        Role role = new Role(); role.setCode("RH"); role.setLibelle("Ressources humaines");
        Utilisateur user = new Utilisateur(); user.setId(1); user.setEmail("test@example.com");
        user.setRole(role); user.setDepartement(departement); user.setActif(true);
        when(auth.currentUser(any())).thenReturn(Optional.of(user));
        Poste poste = new Poste(); poste.setId(2); poste.setIntitule("Développeur Java");
        poste.setDescription("Java et Spring <script>alert(1)</script>");
        StatutOffre statut = new StatutOffre(); statut.setCode("EN_ATTENTE_FINANCE"); statut.setLibelle("En attente Finance");
        offre = new OffreEmploi(); offre.setId(7); offre.setPoste(poste); offre.setDepartement(departement);
        offre.setManager(user); offre.setStatut(statut); offre.setNbPlaces(2); offre.setAnneesExperience(1);
        offre.setSalaire(new BigDecimal("1500000")); offre.setCreatedAt(LocalDateTime.of(2026,11,1,9,0));
        when(offres.toutesOffres()).thenReturn(List.of(offre));
        when(offres.offresDuManager(anyInt())).thenReturn(List.of(offre));
        when(offres.listerOffresPubliees()).thenReturn(List.of(offre));
        when(offres.offresPubliees()).thenReturn(List.of(offre));
        when(offres.postesActifs()).thenReturn(List.of(poste));
        when(offres.findOffre(7)).thenReturn(Optional.of(offre));
    }

    private HttpClient client(String role) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        get(client,"/__ui/session?role="+role);
        return client;
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
            .header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void accueilRhEtVuesDedieesSeRendentAvecLeursFormulaires() throws Exception {
        HttpClient client=client("RH");
        String accueil=get(client,"/rh/dashboard").body();
        assertTrue(accueil.contains("Questionnaires à préparer"));
        assertFalse(accueil.contains("name=\"enonce\""));
        for (String vue : List.of("demandes","offres","candidatures","questionnaires","historique","inconnue")) {
            HttpResponse<String> response=get(client,"/rh/dashboard?vue="+vue);
            assertEquals(200,response.statusCode(),response.body());
            assertTrue(response.body().contains("/assets/app.css"));
            assertTrue(response.body().contains("id=\"contenu\""));
            if (vue.equals("questionnaires")) {
                assertTrue(response.body().contains("/rh/offre/7/candidats#questionnaire"));
                assertFalse(response.body().contains("name=\"enonce\""));
            }
        }
    }

    @Test
    void unRhSansFicheEmployeVoitGestionRhSansAccesPersonnel() throws Exception {
        HttpClient client=client("RH");
        HttpResponse<String> response=get(client,"/rh/dashboard");
        assertEquals(200,response.statusCode(),response.body());
        assertTrue(response.body().contains("<span>Gestion</span> RH"));
        assertTrue(response.body().contains("Vue d'ensemble · Gestion RH"));
        assertTrue(response.body().contains("href=\"/rh/conges\""));
        assertFalse(response.body().contains("href=\"/espaceEmploye\""));
        assertFalse(response.body().contains("<<<<<<<"));
        assertEquals(403,get(client,"/espaceEmploye").statusCode());
    }

    @Test
    void unRhAvecFicheEmployeRetrouveSonEspacePersonnel() throws Exception {
        Employe employe=new Employe(); employe.setId(10); employe.setUtilisateurId(1);
        when(employes.findByUtilisateurId(1)).thenReturn(Optional.of(employe));
        String page=get(client("RH"),"/rh/dashboard").body();
        assertTrue(page.contains("<span>Gestion</span> RH"));
        assertEquals(2,page.split("href=\"/espaceEmploye\"",-1).length-1);
    }

    @Test
    void leMenuCandidatConserveLesLiensEtLaRestrictionEmploye() throws Exception {
        HttpClient client=client("CANDIDAT");
        HttpResponse<String> response=get(client,"/espaceCandidat");
        assertEquals(200,response.statusCode(),response.body());
        assertTrue(response.body().contains("data-candidate-tab=\"cand\""));
        assertTrue(response.body().contains("Conditions et contrats"));
        assertFalse(response.body().contains("Espace employé →"));
        assertFalse(response.body().contains("<<<<<<<"));
        Employe employe=new Employe(); employe.setUtilisateurId(1);
        when(employes.findByUtilisateurId(1)).thenReturn(Optional.of(employe));
        assertTrue(get(client,"/espaceCandidat").body().contains("Espace employé →"));
    }

    @Test
    void lesEspacesEtLeCataloguePartagentLeDesignEtDesLiensUtilisables() throws Exception {
        for (var page : Map.of("MANAGER","/manager/dashboard","FINANCE","/finance/dashboard",
                "DIRECTION","/direction/dashboard","CANDIDAT","/candidat/offres").entrySet()) {
            HttpResponse<String> response=get(client(page.getKey()),page.getValue());
            assertEquals(200,response.statusCode(),response.body());
            assertTrue(response.body().contains("app-sidebar"));
            assertTrue(response.body().contains("/assets/app.js"));
        }
        HttpResponse<String> catalogue=get(HttpClient.newHttpClient(),"/jobOffre");
        assertEquals(200,catalogue.statusCode(),catalogue.body());
        assertTrue(catalogue.body().contains("/espaceCandidat?offre=7"));
        assertTrue(catalogue.body().contains("data-collection"));
        assertFalse(catalogue.body().contains("<script>alert(1)</script>"));
        assertFalse(catalogue.body().contains("/register"));
    }

    @Test
    void erreurMetierConserveLesValeursEtLeContexteDuFormulaire() throws Exception {
        doThrow(new IllegalArgumentException("Vérifiez le salaire proposé.")).when(offres)
            .rhValiderOffre(eq(7),any(),any(),any(),any(),any(),eq(true),any());
        HttpClient client=client("RH");
        HttpResponse<String> submitted=post(client,"/rh/offre/7/valider",
            "salaire=1500000&pointageCsrf=secret-a-ne-pas-restaurer&commentaire="+URLEncoder.encode("À revoir <script>alert(1)</script>",StandardCharsets.UTF_8));
        assertEquals(302,submitted.statusCode());
        assertTrue(submitted.headers().firstValue("Location").orElseThrow().endsWith("/rh/dashboard?vue=offres"));
        String page=get(client,"/rh/dashboard?vue=offres").body();
        assertTrue(page.contains("Vérifiez le salaire proposé."));
        assertTrue(page.contains("id=\"app-form-restore\""));
        assertTrue(page.contains("data-restore-name=\"salaire\""));
        assertFalse(page.contains("<script>alert(1)</script>"));
        assertFalse(page.contains("secret-a-ne-pas-restaurer"));
        assertFalse(get(client,"/rh/dashboard?vue=offres").body().contains("id=\"app-form-restore\""));
    }

    @Test
    void uneActionDepuisLaFicheOffreRevientALaMemeFiche() throws Exception {
        HttpClient client=client("RH");
        HttpResponse<String> response=post(client,"/rh/candidature/4/eliminer","retourOffre=7");
        assertEquals(302,response.statusCode());
        assertTrue(response.headers().firstValue("Location").orElseThrow().endsWith("/rh/offre/7/candidats"));
        HttpResponse<String> fiche=get(client,"/rh/offre/7/candidats");
        assertEquals(200,fiche.statusCode(),fiche.body());
        assertTrue(fiche.body().contains("data-question"));
        assertTrue(fiche.body().contains("Énoncé"));
        assertTrue(fiche.body().contains("Réponses séparées par |"));
        assertTrue(fiche.body().contains("name=\"retourOffre\" value=\"7\""));
    }

    @Test
    void connexionConserveUniquementLEmailEtNePrefillPasLeMotDePasse() throws Exception {
        HttpResponse<String> response=post(HttpClient.newHttpClient(),"/login","email=test%40example.com&password=wrong");
        assertEquals(200,response.statusCode(),response.body());
        assertTrue(response.body().contains("value=\"test@example.com\""));
        assertTrue(response.body().contains("autocomplete=\"current-password\""));
        assertFalse(response.body().contains("value=\"password\""));
    }

    @Test
    void uneOffreSelectionneeEstConserveeApresConnexion() throws Exception {
        var user=new Utilisateur(); user.setId(1);
        var role=new Role(); role.setCode("CANDIDAT"); user.setRole(role);
        when(auth.authenticate("candidat@example.com","correct")).thenReturn(Optional.of(user));
        HttpClient loginClient=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String login=get(loginClient,"/login?offre=7").body();
        assertTrue(login.contains("name=\"offre\" value=\"7\""));
        HttpResponse<String> response=post(loginClient,"/login","email=candidat%40example.com&password=correct&offre=7");
        assertEquals(302,response.statusCode());
        assertTrue(response.headers().firstValue("Location").orElseThrow().endsWith("/espaceCandidat?offre=7"));
    }

    @Test
    void seulUnPointageOuvertAfficheLHeureEtLaConfirmationDeSortie() throws Exception {
        Pointage pointage=new Pointage(); pointage.setConnexion(LocalDateTime.of(2026,11,12,8,2));
        when(pointages.findByJeton("ui-pointage")).thenReturn(Optional.of(pointage));
        HttpClient client=client("RH");
        get(client,"/__ui/session?role=RH&pointage=true");
        String page=get(client,"/rh/dashboard").body();
        assertTrue(page.contains("Pointage en cours depuis 12/11 à 08:02"));
        assertTrue(page.contains("data-pointage-confirm"));
        pointage.setSessionExpiree(true);
        assertFalse(get(client,"/rh/dashboard").body().contains("data-pointage-confirm"));
    }

    @Test
    void lesEcheancesAffichentLesDocumentsProchesSansInclureLesDatesHorsPeriode() throws Exception {
        var date=java.time.LocalDate.now(clock);
        jdbc.update("insert into utilisateur(id,email,actif) values(9001,'echeance@example.com',true)");
        try {
            for (int offset : List.of(-1,2,31)) {
                jdbc.update("insert into document_administratif(id,id_utilisateur,id_deposant,type,nom_fichier,type_mime,contenu,date_expiration,depose_le) values(?,9001,9001,'CV','cv.pdf','application/pdf',?,?,?)",
                    9100+offset,new byte[]{1},date.plusDays(offset),LocalDateTime.now(clock));
            }
            var echeances=dashboard.prochainesEcheances();
            assertEquals(1,echeances.size());
            assertEquals("echeance@example.com",echeances.getFirst().get("description"));
            assertEquals("/rh/dossiers/9001#documents",echeances.getFirst().get("lien"));
            String page=get(client("RH"),"/rh/dashboard").body();
            assertTrue(page.contains("Document à renouveler · CV"));
            assertTrue(page.contains("echeance@example.com"));
        } finally {
            jdbc.update("delete from document_administratif where id_utilisateur=9001");
            jdbc.update("delete from utilisateur where id=9001");
        }
    }
}
