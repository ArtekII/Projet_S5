package com.project.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.conditions.publication-active", havingValue = "true")
public class PublicationConditionsTask {
    private final ConditionTravailService service;

    public PublicationConditionsTask(ConditionTravailService service) { this.service = service; }

    // Publie les conditions sans attendre que le candidat ouvre sa page.
    @Scheduled(fixedDelayString = "${app.conditions.publication-intervalle-ms:60000}", initialDelay = 10000)
    public void publier() { service.publierConditionsDisponibles(); }
}
