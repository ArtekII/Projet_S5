package com.project.controller;

import com.project.repository.PointageRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Display the recorded entry, rather than inferring a pointage from authentication. */
@ControllerAdvice(basePackages = "com.project.controller")
public class AppViewAdvice {
    private final PointageRepository pointages;

    public AppViewAdvice(PointageRepository pointages) { this.pointages = pointages; }

    @ModelAttribute
    public void pointage(HttpServletRequest request, Model model) {
        var session = request.getSession(false);
        if (session == null || !(session.getAttribute("pointageToken") instanceof String jeton)) return;
        pointages.findByJeton(jeton).filter(p -> p.getDeconnexion() == null && !p.isRegularise() && !p.isSessionExpiree())
            .ifPresent(p -> model.addAttribute("pointageSessionVue",
                p.getConnexion().format(DateTimeFormatter.ofPattern("dd/MM à HH:mm"))));
    }
}
