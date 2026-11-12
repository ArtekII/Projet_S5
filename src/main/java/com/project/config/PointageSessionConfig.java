package com.project.config;

import com.project.service.PointageService;
import jakarta.servlet.http.*;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class PointageSessionConfig {
    private volatile boolean arretEnCours;

    @EventListener
    public void arret(ContextClosedEvent evenement) { arretEnCours = true; }

    @Bean
    ServletListenerRegistrationBean<HttpSessionListener> pointageSessionListener(PointageService pointages) {
        return new ServletListenerRegistrationBean<>(new HttpSessionListener() {
            @Override public void sessionDestroyed(HttpSessionEvent evenement) {
                // A l'arret, la detection au redemarrage traitera les sessions sans sortie.
                if (arretEnCours) return;
                try {
                    String jeton = (String) evenement.getSession().getAttribute("pointageToken");
                    if (jeton != null) pointages.expirerSession(jeton);
                } catch (RuntimeException ex) {
                    LoggerFactory.getLogger(PointageSessionConfig.class).warn("Impossible de signaler une expiration de session de pointage", ex);
                }
            }
        });
    }
}
