package com.project.controller;

import com.project.entity.Employe;
import com.project.entity.Role;
import com.project.entity.Utilisateur;
import com.project.repository.EmployeRepository;
import com.project.repository.UtilisateurRepository;
import com.project.service.AuthService;
import com.project.service.PointageService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthRedirectTest {
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode("password");
    @Mock UtilisateurRepository utilisateurs;
    @Mock EmployeRepository employes;
    @Mock PointageService pointages;
    private MockMvc mvc;

    @BeforeEach
    void configuration() {
        var auth = new AuthService(utilisateurs, pointages, employes);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth)).build();
    }

    private void utilisateur(String codeRole) {
        Role role = new Role(); role.setCode(codeRole); role.setLibelle(codeRole);
        Utilisateur user = new Utilisateur(); user.setId(42); user.setRole(role);
        user.setEmail("compte@example.com"); user.setMotDePasse(PASSWORD_HASH); user.setActif(true);
        when(utilisateurs.findByEmail("compte@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void unEmployeArriveDansSonEspaceQuelQueSoitSonRole() throws Exception {
        when(employes.findByUtilisateurId(42)).thenReturn(Optional.of(new Employe()));
        for (String role : new String[]{"RH", "MANAGER", "FINANCE", "DIRECTION", "CANDIDAT"}) {
            utilisateur(role);
            var session = new MockHttpSession();
            mvc.perform(post("/login").session(session).param("email", "compte@example.com").param("password", "password"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/espaceEmploye"))
                .andExpect(request().sessionAttribute("userRole", role));
            mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/espaceEmploye"));
            mvc.perform(get("/login").session(session)).andExpect(redirectedUrl("/espaceEmploye"));
        }
    }

    @Test
    void unCompteSansFicheEmployeConserveSonAccueilMetier() throws Exception {
        when(employes.findByUtilisateurId(42)).thenReturn(Optional.empty());
        var destinations = Map.of("RH", "/rh/dashboard", "MANAGER", "/manager/dashboard",
            "FINANCE", "/finance/dashboard", "DIRECTION", "/direction/dashboard", "CANDIDAT", "/espaceCandidat");
        for (var entry : destinations.entrySet()) {
            utilisateur(entry.getKey());
            var session = new MockHttpSession();
            mvc.perform(post("/login").session(session).param("email", "compte@example.com").param("password", "password"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(entry.getValue()));
            mvc.perform(get("/").session(session)).andExpect(redirectedUrl(entry.getValue()));
            mvc.perform(get("/login").session(session)).andExpect(redirectedUrl(entry.getValue()));
        }
    }

    @Test
    void uneOffreChoisieExplicitementRestePrioritairePourLeCandidat() throws Exception {
        utilisateur("CANDIDAT");
        var session = new MockHttpSession();
        mvc.perform(post("/login").session(session).param("email", "compte@example.com")
                .param("password", "password").param("offre", "7"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/espaceCandidat?offre=7"));
        mvc.perform(get("/login").session(session).param("offre", "7"))
            .andExpect(redirectedUrl("/espaceCandidat?offre=7"));
    }
}
