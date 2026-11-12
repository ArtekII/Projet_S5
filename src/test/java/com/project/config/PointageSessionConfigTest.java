package com.project.config;

import com.project.service.PointageService;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.*;

class PointageSessionConfigTest {
    @Test void uneExpirationSignaleUniquementLaSessionConcernee() {
        PointageService service = mock(PointageService.class);
        var listener = new PointageSessionConfig().pointageSessionListener(service).getListener();
        MockHttpSession session = new MockHttpSession();
        listener.sessionDestroyed(new HttpSessionEvent(session)); verifyNoInteractions(service);
        session.setAttribute("pointageToken", "jeton-session");
        listener.sessionDestroyed(new HttpSessionEvent(session)); verify(service).expirerSession("jeton-session");
    }

    @Test void larretNeSollicitePasUneBaseEnCoursDeFermeture() {
        PointageService service = mock(PointageService.class);
        var configuration = new PointageSessionConfig();
        var listener = configuration.pointageSessionListener(service).getListener();
        MockHttpSession session = new MockHttpSession(); session.setAttribute("pointageToken", "jeton-session");
        try (var contexte = new StaticApplicationContext()) {
            configuration.arret(new ContextClosedEvent(contexte));
            listener.sessionDestroyed(new HttpSessionEvent(session)); verifyNoInteractions(service);
        }
    }
}
