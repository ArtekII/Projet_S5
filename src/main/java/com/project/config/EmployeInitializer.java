package com.project.config;

import com.project.repository.ContratRepository;
import com.project.service.EmployeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Rattrape les contrats existants, sans modifier leur type. */
@Component
public class EmployeInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(EmployeInitializer.class);
    private final ContratRepository contrats;
    private final EmployeService employes;

    public EmployeInitializer(ContratRepository contrats, EmployeService employes) {
        this.contrats = contrats;
        this.employes = employes;
    }

    @Override
    public void run(String... args) {
        // Le plus recent devient le contrat actuel pour une personne non encore enregistree.
        contrats.findAllByOrderByIdDesc().stream()
                .filter(c -> "FINALISE".equals(c.getStatut()))
                .forEach(contrat -> {
                    // Chaque appel a sa transaction : un ancien dossier incomplet
                    // ne doit pas annuler les autres embauches ni arreter le serveur.
                    try {
                        employes.assurerPourContrat(contrat);
                    } catch (EmployeService.DossierCvManquantException e) {
                        log.warn("Employe non cree pour le contrat {} : {} Corriger le dossier puis redemarrer.",
                                contrat.getId(), e.getMessage());
                    }
                });
    }
}
