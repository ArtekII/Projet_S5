package com.project.service;

import com.project.entity.*;
import com.project.repository.*;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
        "app.conditions.publication-active=false", "app.contrats.activation-delai-initial-ms=3600000",
        "spring.datasource.url=jdbc:h2:mem:dossiers;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class DossierAdministratifTest {
    @Autowired DossierAdministratifService service;
    @Autowired ContratEmployeService contrats;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired RoleRepository roles;
    @Autowired DossierAdministratifRepository dossiers;
    @Autowired DocumentAdministratifRepository documents;
    @Autowired ParcoursProfessionnelRepository parcours;
    @Autowired JournalDossierRepository journal;
    @Autowired CvRepository cvs;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockBean Clock clock;
    @LocalServerPort int port;
    Utilisateur rh, titulaire, autre;

    @BeforeEach
    void preparer() {
        when(clock.getZone()).thenReturn(ZoneId.of("Indian/Antananarivo"));
        when(clock.instant()).thenReturn(Instant.parse("2026-11-08T09:00:00Z"));
        rh = utilisateur("RH"); titulaire = utilisateur("CANDIDAT"); autre = utilisateur("MANAGER");
    }

    private Utilisateur utilisateur(String code) {
        Role role = roles.findByCode(code).orElseGet(() -> roles.saveAndFlush(new Role(code, code)));
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(UUID.randomUUID() + "@example.com"); utilisateur.setRole(role);
        utilisateur.setMotDePasse(new BCryptPasswordEncoder().encode("secret"));
        return utilisateurs.saveAndFlush(utilisateur);
    }

    private DossierAdministratif fiche() {
        DossierAdministratif fiche = new DossierAdministratif();
        fiche.setNom("Rakoto <script>alert(1)</script>"); fiche.setPrenom("Onja");
        fiche.setDateNaissance(LocalDate.of(1995, 1, 2)); fiche.setAdresse("Antananarivo");
        fiche.setTelephone("0341234567"); fiche.setEmailPersonnel(titulaire.getEmail());
        fiche.setSituationMatrimoniale("CELIBATAIRE"); fiche.setNombreEnfants(0);
        return fiche;
    }

    private int dernierId(String table) {
        return jdbc.queryForObject("select max(id) from " + table, Integer.class);
    }

    private int embaucher() {
        jdbc.update("insert into poste (intitule) values ('Développeur')"); int poste = dernierId("poste");
        jdbc.update("insert into offre_emploi (id_poste) values (?)", poste); int offre = dernierId("offre_emploi");
        jdbc.update("insert into cv_candidat (id_candidat, id_offre, nom, prenom, annee_experience, created_at) values (?, ?, 'Rakoto', 'Onja', 2, current_timestamp)", titulaire.getId(), offre);
        int cv = dernierId("cv_candidat");
        Integer decision = jdbc.query("select id from decision where code = 'VALIDEE'", rs -> rs.next() ? rs.getInt(1) : null);
        if (decision == null) { jdbc.update("insert into decision (code, libelle) values ('VALIDEE', 'Validée')"); decision = dernierId("decision"); }
        Integer type = jdbc.query("select id from type_contrat where code = 'CDI'", rs -> rs.next() ? rs.getInt(1) : null);
        if (type == null) { jdbc.update("insert into type_contrat (code, libelle) values ('CDI', 'CDI')"); type = dernierId("type_contrat"); }
        jdbc.update("insert into condition_travail (id_poste, version, id_role_proposant, salaire, heures_par_semaine, lieu_travail, id_decision_rh, created_at) values (?, 1, ?, 1500000, 40, 'Antananarivo', ?, current_timestamp)", poste, rh.getRole().getId(), decision);
        int condition = dernierId("condition_travail");
        jdbc.update("insert into contrat (id_condition, id_type_contrat, date_debut, statut, created_at) values (?, ?, '2026-10-01', 'FINALISE', current_timestamp)", condition, type);
        int contrat = dernierId("contrat");
        jdbc.update("insert into employe (id_utilisateur, id_cv_candidat, id_contrat_actuel, date_embauche) values (?, ?, ?, '2026-10-01')", titulaire.getId(), cv, contrat);
        int employe = jdbc.queryForObject("select id_employe from employe where id_utilisateur = ?", Integer.class, titulaire.getId());
        jdbc.update("update contrat set id_employe = ? where id = ?", employe, contrat);
        return employe;
    }

    private MockMultipartFile pdf(String nom) {
        return new MockMultipartFile("fichier", nom, "application/pdf", "%PDF-1.7\ncontenu test".getBytes(StandardCharsets.UTF_8));
    }

    private MockHttpSession session(Utilisateur utilisateur) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", utilisateur.getId()); session.setAttribute("userRole", utilisateur.getRole().getCode());
        session.setAttribute("dossierCsrf", "test-token");
        return session;
    }

    @Test
    void renseignementsLiesAUtilisateurEtModificationsVersionnees() {
        DossierAdministratif saisie = fiche();
        service.enregistrerFiche(titulaire.getId(), titulaire.getId(), saisie);
        DossierAdministratif sauvegardee = dossiers.findByUtilisateur_Id(titulaire.getId()).orElseThrow();
        assertEquals(titulaire.getId(), sauvegardee.getUtilisateur().getId());
        assertEquals("CELIBATAIRE", sauvegardee.getSituationMatrimoniale());
        assertEquals(LocalDateTime.of(2026, 11, 8, 12, 0), sauvegardee.getModifieLe());
        assertThrows(IllegalArgumentException.class, () -> service.enregistrerFiche(titulaire.getId(), rh.getId(), saisie));
        saisie.setRevision(sauvegardee.getRevision()); saisie.setTelephone("0321234567");
        service.enregistrerFiche(titulaire.getId(), rh.getId(), saisie);
        JournalDossier evenement = journal.findTop100ByUtilisateur_IdOrderByIdDesc(titulaire.getId()).getFirst();
        assertEquals(rh.getId(), evenement.getActeur().getId());
        assertTrue(evenement.getDetail().contains("telephone"));
        assertFalse(evenement.getDetail().contains("0321234567"));
        assertThrows(ResponseStatusException.class, () -> service.consulter(titulaire.getId(), autre.getId()));
        assertThrows(ResponseStatusException.class, () -> service.enregistrerFiche(titulaire.getId(), autre.getId(), fiche()));
        assertThrows(ResponseStatusException.class, () -> service.lister(titulaire.getId()));
        titulaire.setActif(false); utilisateurs.saveAndFlush(titulaire);
        assertThrows(ResponseStatusException.class, () -> service.consulter(titulaire.getId(), titulaire.getId()));
    }

    @Test
    void documentsConservesTelechargementsControlesEtValiditeTracee() throws Exception {
        LocalDate aujourdHui = LocalDate.of(2026, 11, 8);
        assertEquals("EXPIRE", service.validite(aujourdHui.minusDays(1)));
        assertEquals("A_RENOUVELER", service.validite(aujourdHui));
        assertEquals("A_RENOUVELER", service.validite(aujourdHui.plusDays(30)));
        assertEquals("VALIDE", service.validite(aujourdHui.plusDays(31)));
        assertEquals("SANS_ECHEANCE", service.validite(null));
        int premier = service.deposer(titulaire.getId(), titulaire.getId(), "CV", pdf("C:\\faux\\cv.pdf"), null, aujourdHui.minusDays(1));
        int second = service.deposer(titulaire.getId(), rh.getId(), "CV", pdf("cv-actualise.pdf"), aujourdHui, aujourdHui.plusDays(30));
        assertNotEquals(premier, second);
        assertEquals("cv.pdf", service.telecharger(titulaire.getId(), rh.getId(), premier).getNomFichier());
        assertArrayEquals(pdf("cv.pdf").getBytes(), service.telecharger(titulaire.getId(), titulaire.getId(), premier).getContenu());
        assertThrows(ResponseStatusException.class, () -> service.telecharger(titulaire.getId(), autre.getId(), premier));
        assertThrows(ResponseStatusException.class, () -> service.telecharger(autre.getId(), rh.getId(), premier));
        assertThrows(ResponseStatusException.class, () -> service.modifierValidite(titulaire.getId(), titulaire.getId(), premier, null, null));
        service.modifierValidite(titulaire.getId(), rh.getId(), premier, null, aujourdHui.plusDays(60));
        var modification = journal.findTop100ByUtilisateur_IdOrderByIdDesc(titulaire.getId()).getFirst();
        assertEquals("VALIDITE_DOCUMENT", modification.getAction());
        assertTrue(modification.getDetail().contains("2026-11-07"));
        assertTrue(modification.getDetail().contains("2027-01-07"));
        var vue = service.consulter(titulaire.getId(), rh.getId());
        var pieces = (List<?>) vue.get("documents"); assertEquals(2, pieces.size());
        assertFalse(((Map<?, ?>) pieces.getFirst()).containsKey("contenu"));
        var ligne = service.lister(rh.getId()).stream().filter(d -> ((Number) d.get("id")).intValue() == titulaire.getId()).findFirst().orElseThrow();
        assertEquals(1L, ((Number) ligne.get("aRenouveler")).longValue());
        assertEquals(0L, ((Number) ligne.get("expires")).longValue());
    }

    @Test
    void refuseFichiersEtPeriodesInvalides() {
        assertThrows(IllegalArgumentException.class, () -> service.deposer(titulaire.getId(), titulaire.getId(), "INCONNU", pdf("cv.pdf"), null, null));
        assertThrows(IllegalArgumentException.class, () -> service.deposer(titulaire.getId(), titulaire.getId(), "CV", pdf("cv.exe"), null, null));
        assertThrows(IllegalArgumentException.class, () -> service.deposer(titulaire.getId(), titulaire.getId(), "CV", new MockMultipartFile("fichier", "faux.pdf", "application/pdf", "<script>".getBytes()), null, null));
        assertThrows(IllegalArgumentException.class, () -> service.deposer(titulaire.getId(), titulaire.getId(), "CV", new MockMultipartFile("fichier", "cv.pdf", "application/pdf", new byte[(int) DossierAdministratifService.TAILLE_MAX + 1]), null, null));
        assertThrows(IllegalArgumentException.class, () -> service.deposer(titulaire.getId(), titulaire.getId(), "CV", pdf("cv.pdf"), LocalDate.of(2026, 11, 9), null));
        assertThrows(IllegalArgumentException.class, () -> service.deposer(titulaire.getId(), titulaire.getId(), "CV", pdf("cv.pdf"), LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 7)));
    }

    @Test
    void preRemplitDepuisCvSansModifierLeRecrutementEtValideLeParcours() {
        Cv cv = new Cv(); cv.setCandidat(titulaire); cv.setNom("Nom du CV"); cv.setPrenom("Prénom du CV");
        cv.setDateNaissance(LocalDate.of(1995, 1, 2)); cv.setDiplome("Licence"); cv.setCompetences("Java");
        Cv sauvegarde = cvs.saveAndFlush(cv);
        var vue = service.consulter(titulaire.getId(), titulaire.getId());
        assertEquals("Nom du CV", ((DossierAdministratif) vue.get("fiche")).getNom());
        assertEquals(1, ((List<?>) vue.get("cvs")).size());
        assertTrue(dossiers.findByUtilisateur_Id(titulaire.getId()).isEmpty());
        service.enregistrerFiche(titulaire.getId(), titulaire.getId(), fiche());
        assertEquals("Nom du CV", cvs.findById(sauvegarde.getId()).orElseThrow().getNom());
        ParcoursProfessionnel evenement = new ParcoursProfessionnel();
        evenement.setType("SALAIRE"); evenement.setLibelle("Salaire mensuel"); evenement.setDateDebut(LocalDate.of(2026, 11, 8));
        assertThrows(IllegalArgumentException.class, () -> service.ajouterParcours(titulaire.getId(), rh.getId(), evenement));
        embaucher();
        assertThrows(IllegalArgumentException.class, () -> service.ajouterParcours(titulaire.getId(), rh.getId(), evenement));
        evenement.setMontant(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> service.ajouterParcours(titulaire.getId(), rh.getId(), evenement));
        evenement.setMontant(new BigDecimal("1000000")); evenement.setDateFin(LocalDate.of(2026, 11, 7));
        assertThrows(IllegalArgumentException.class, () -> service.ajouterParcours(titulaire.getId(), rh.getId(), evenement));
        evenement.setDateFin(null);
        assertThrows(ResponseStatusException.class, () -> service.ajouterParcours(titulaire.getId(), titulaire.getId(), evenement));
        assertEquals(0, parcours.findByUtilisateur_IdOrderByDateDebutDescIdDesc(titulaire.getId()).size());
    }

    @Test
    void centraliseCvContratsSalairesParcoursEtChecklistPersistante() {
        int employeId = embaucher();
        service.enregistrerFiche(titulaire.getId(), rh.getId(), fiche());
        Long revision = dossiers.findByUtilisateur_Id(titulaire.getId()).orElseThrow().getRevision();
        service.enregistrerIntegration(titulaire.getId(), rh.getId(), revision, Set.of("signature", "badge", "compte", "materiel", "formation", "paie"));
        DossierAdministratif dossier = dossiers.findByUtilisateur_Id(titulaire.getId()).orElseThrow();
        assertTrue(dossier.isSignature() && dossier.isBadge() && dossier.isCompte() && dossier.isMateriel() && dossier.isFormation() && dossier.isPaie());
        assertThrows(IllegalArgumentException.class, () -> service.enregistrerIntegration(titulaire.getId(), rh.getId(), revision, Set.of()));
        service.enregistrerIntegration(titulaire.getId(), rh.getId(), dossier.getRevision(), Set.of("signature"));
        assertFalse(dossiers.findByUtilisateur_Id(titulaire.getId()).orElseThrow().isBadge());
        assertThrows(ResponseStatusException.class, () -> service.enregistrerIntegration(titulaire.getId(), titulaire.getId(), dossier.getRevision(), Set.of()));
        for (String type : List.of("AFFECTATION", "SALAIRE", "EXPERIENCE")) {
            ParcoursProfessionnel entree = new ParcoursProfessionnel(); entree.setType(type); entree.setLibelle("Historique " + type);
            entree.setDateDebut(LocalDate.of(2026, 10, 1));
            if (type.equals("SALAIRE")) entree.setMontant(new BigDecimal("1600000.00"));
            service.ajouterParcours(titulaire.getId(), rh.getId(), entree);
        }
        Contrat futur = contrats.programmer(employeId, "CDI", LocalDate.of(2026, 11, 9), null, rh.getId());
        var vue = service.consulter(titulaire.getId(), titulaire.getId());
        assertFalse(vue.containsKey("journal"));
        assertEquals(1, ((List<?>) vue.get("cvsCandidats")).size());
        assertEquals(3, ((List<?>) vue.get("parcours")).size());
        assertEquals(2, ((List<?>) vue.get("contrats")).size());
        assertTrue(((List<?>) vue.get("contrats")).stream().map(v -> (Map<?, ?>) v).allMatch(v -> new BigDecimal("1500000.00").compareTo((BigDecimal) v.get("salaire")) == 0));
        when(clock.instant()).thenReturn(Instant.parse("2026-11-09T09:00:00Z"));
        assertEquals(1, contrats.activerContratsDus());
        assertEquals(futur.getId(), jdbc.queryForObject("select id_contrat_actuel from employe where id_employe = ?", Integer.class, employeId));
        assertEquals(2, contrats.historique(employeId).size());
        var integration = journal.findTop100ByUtilisateur_IdOrderByIdDesc(titulaire.getId()).stream().filter(j -> j.getAction().equals("CHECKLIST_INTEGRATION")).toList();
        assertEquals(2, integration.size());
        assertTrue(integration.getFirst().getDetail().contains("badge=false"));
    }

    @Test
    void formulairesProtegesEtRattachementsNonModifiables() throws Exception {
        mvc.perform(get("/dossier")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/espaceEmploye")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/rh/dossiers").session(session(titulaire))).andExpect(status().is3xxRedirection());
        MockHttpSession session = session(titulaire);
        mvc.perform(post("/espaceEmploye/fiche").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/espaceEmploye/fiche").session(session).param("dossierCsrf", "test-token")
                .param("nom", "Rakoto").param("prenom", "Onja").param("dateNaissance", "1995-01-02")
                .param("adresse", "Antananarivo").param("telephone", "0341234567").param("emailPersonnel", titulaire.getEmail())
                .param("situationMatrimoniale", "CELIBATAIRE").param("utilisateur.id", autre.getId().toString())
                .param("signature", "true").param("paie", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/espaceEmploye"));
        var fiche = dossiers.findByUtilisateur_Id(titulaire.getId()).orElseThrow();
        assertFalse(fiche.isSignature()); assertFalse(fiche.isPaie());
        assertTrue(dossiers.findByUtilisateur_Id(autre.getId()).isEmpty());
        mvc.perform(multipart("/espaceEmploye/documents").file(pdf("cv.pdf")).session(session)
                .param("type", "CV").param("dossierCsrf", "test-token"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/espaceEmploye?onglet=documents"));
        int document = jdbc.queryForObject("select max(id) from document_administratif where id_utilisateur = ?", Integer.class, titulaire.getId());
        mvc.perform(get("/espaceEmploye/documents/" + document).session(session(autre))).andExpect(status().isNotFound());
        mvc.perform(get("/espaceEmploye/documents/" + document).session(session)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(pdf("cv.pdf").getBytes()));
        mvc.perform(post("/rh/dossiers/" + titulaire.getId() + "/integration").session(session)
                .param("dossierCsrf", "test-token")).andExpect(status().is3xxRedirection());
        assertFalse(dossiers.findByUtilisateur_Id(titulaire.getId()).orElseThrow().isPaie());
        mvc.perform(get("/rh/dossiers/" + titulaire.getId()).session(session(rh)))
                .andExpect(status().isOk()).andExpect(view().name("dossier/detail"));
        embaucher();
        mvc.perform(post("/rh/dossiers/" + titulaire.getId() + "/integration").session(session(rh))
                .param("dossierCsrf", "test-token").param("revision", fiche.getRevision().toString()))
                .andExpect(status().is3xxRedirection());
        assertFalse(dossiers.findByUtilisateur_Id(titulaire.getId()).orElseThrow().isSignature());
        mvc.perform(get("/dossier").session(session)).andExpect(status().isOk())
                .andExpect(view().name("employe/espaceEmploye"));
        mvc.perform(get("/espaceEmploye").session(session).param("onglet", "inconnu"))
                .andExpect(status().isOk()).andExpect(model().attribute("ongletEmploye", "renseignements"));
        mvc.perform(get("/espaceEmploye").session(session(rh))).andExpect(status().isOk())
                .andExpect(model().attribute("gestionRh", false));
    }

    private HttpResponse<String> requete(HttpClient client, HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }

    private HttpClient client(Utilisateur utilisateur) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        var response = requete(client, HttpRequest.newBuilder(uri("/login")).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=" + URLEncoder.encode(utilisateur.getEmail(), StandardCharsets.UTF_8) + "&password=secret")).build());
        assertEquals(302, response.statusCode());
        return client;
    }

    @Test
    void pagesJspReellesAffichentLesDonneesEchappeesEtLesErreurs() throws Exception {
        embaucher(); service.enregistrerFiche(titulaire.getId(), titulaire.getId(), fiche());
        service.deposer(titulaire.getId(), titulaire.getId(), "IDENTITE", pdf("<script>.pdf"), null, LocalDate.of(2026, 11, 7));
        HttpClient rhClient = client(rh);
        HttpResponse<String> detail = requete(rhClient, HttpRequest.newBuilder(uri("/rh/dossiers/" + titulaire.getId())).GET().build());
        assertEquals(200, detail.statusCode(), detail.body());
        assertTrue(detail.body().contains("Checklist d'intégration"));
        assertTrue(detail.body().contains("Journal des accès et modifications"));
        assertTrue(detail.body().contains("&lt;script&gt;")); assertFalse(detail.body().contains("<script>"));
        assertTrue(detail.body().contains("Expiré"));
        assertEquals("no-store", detail.headers().firstValue("cache-control").orElseThrow());
        var liste = requete(rhClient, HttpRequest.newBuilder(uri("/rh/dossiers")).GET().build());
        assertEquals(200, liste.statusCode(), liste.body()); assertTrue(liste.body().contains("1 expiré(s)"));
        HttpClient propreClient = client(titulaire);
        var propre = requete(propreClient, HttpRequest.newBuilder(uri("/espaceEmploye")).GET().build());
        assertEquals(200, propre.statusCode(), propre.body()); assertFalse(propre.body().contains("Journal des accès et modifications"));
        assertTrue(propre.body().contains("<title>Espace employé – JobListe</title>"));
        assertTrue(propre.body().contains("class=\"employe-side\""));
        assertTrue(propre.body().contains("action=\"/espaceEmploye/fiche\""));
        var matcher = java.util.regex.Pattern.compile("name=\"dossierCsrf\" value=\"([^\"]+)\"").matcher(propre.body());
        assertTrue(matcher.find());
        var erreur = requete(propreClient, HttpRequest.newBuilder(uri("/dossier/fiche"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("dossierCsrf=" + matcher.group(1) + "&dateNaissance=incorrecte&nom=Conserve" )).build());
        assertEquals(200, erreur.statusCode(), erreur.body());
        assertTrue(erreur.body().contains("value=\"Conserve\""));
        assertTrue(erreur.body().contains("alert alert-danger"));
        assertTrue(erreur.body().contains("Espace employé"));
        assertFalse(erreur.body().contains("java.time.LocalDate"));
        for (String onglet : List.of("documents", "cv", "contrats", "parcours", "integration")) {
            var page = requete(propreClient, HttpRequest.newBuilder(uri("/espaceEmploye?onglet=" + onglet)).GET().build());
            assertEquals(200, page.statusCode(), page.body());
            assertTrue(page.body().contains("id=\"" + onglet + "\""), page.body());
            assertFalse(page.body().contains("id=\"renseignements\""));
            assertFalse(page.body().contains("Journal des accès et modifications"));
            if (onglet.equals("documents")) {
                assertTrue(page.body().contains("&lt;script&gt;.pdf"));
                assertTrue(page.body().contains("action=\"/espaceEmploye/documents\""));
                assertTrue(page.body().contains("Expiré"));
            }
        }
    }
}
