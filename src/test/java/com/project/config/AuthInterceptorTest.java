package com.project.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class AuthInterceptorTest {
    @Test
    void protegeLesRoutesRhAvecUnContexteDeDeploiement() throws Exception {
        var request = new MockHttpServletRequest("POST", "/recrutement/rh/contrats/1/preparer");
        request.setContextPath("/recrutement");
        request.getSession().setAttribute("userId", 100);
        request.getSession().setAttribute("userRole", "CANDIDAT");
        var response = new MockHttpServletResponse();
        assertFalse(new AuthInterceptor().preHandle(request, response, new Object()));
        assertEquals(302, response.getStatus());
        request.getSession().setAttribute("userRole", "RH");
        assertTrue(new AuthInterceptor().preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
