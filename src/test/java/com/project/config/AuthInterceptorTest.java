package com.project.config;

import org.junit.jupiter.api.Test;
import com.project.repository.EmployeRepository;
import com.project.entity.Employe;
import java.util.Optional;
import java.lang.reflect.Proxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class AuthInterceptorTest {
    private boolean employePresent;
    private final EmployeRepository employes = (EmployeRepository) Proxy.newProxyInstance(
            EmployeRepository.class.getClassLoader(), new Class<?>[]{EmployeRepository.class},
            (proxy, method, args) -> {
                if (method.getName().equals("findByUtilisateurId")) {
                    return employePresent ? Optional.of(new Employe()) : Optional.empty();
                }
                throw new UnsupportedOperationException(method.getName());
            });
    private final AuthInterceptor interceptor = new AuthInterceptor(employes);

    @Test
    void refuseLesNonEmployesSurToutesLesRoutesPersonnelles() throws Exception {
        for (String path : new String[]{"/espaceEmploye", "/espaceEmploye/conges", "/dossier", "/dossier/documents/1"}) {
            for (String role : new String[]{"RH", "CANDIDAT", "MANAGER", "FINANCE", "DIRECTION"}) {
                var request = new MockHttpServletRequest("GET", "/recrutement" + path);
                request.setContextPath("/recrutement");
                request.getSession().setAttribute("userId", 100);
                request.getSession().setAttribute("userRole", role);
                var response = new MockHttpServletResponse();
                assertFalse(interceptor.preHandle(request, response, new Object()));
                assertEquals(403, response.getStatus());
                assertEquals(false, request.getAttribute("accesEspaceEmploye"));
            }
        }
    }

    @Test
    void autoriseUnEmployeEtAfficheSonLien() throws Exception {
        employePresent = true;
        var request = new MockHttpServletRequest("GET", "/espaceEmploye");
        request.getSession().setAttribute("userId", 100);
        request.getSession().setAttribute("userRole", "CANDIDAT");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(true, request.getAttribute("accesEspaceEmploye"));
    }

    @Test
    void autoriseUnRhQuiEstAussiEmploye() throws Exception {
        employePresent = true;
        var request = new MockHttpServletRequest("GET", "/espaceEmploye");
        request.getSession().setAttribute("userId", 100);
        request.getSession().setAttribute("userRole", "RH");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(true, request.getAttribute("accesEspaceEmploye"));
    }

    @Test
    void protegeLesRoutesRhAvecUnContexteDeDeploiement() throws Exception {
        var request = new MockHttpServletRequest("POST", "/recrutement/rh/contrats/1/preparer");
        request.setContextPath("/recrutement");
        request.getSession().setAttribute("userId", 100);
        request.getSession().setAttribute("userRole", "CANDIDAT");
        var response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(302, response.getStatus());
        request.getSession().setAttribute("userRole", "RH");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
