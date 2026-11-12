package com.project.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Recalcule regulierement les soldes de conges en cours. */
@Component
public class SoldeCongeTask {
    private final CongeService service;

    public SoldeCongeTask(CongeService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.conges.actualisation-intervalle-ms:3600000}",
            initialDelayString = "${app.conges.actualisation-delai-initial-ms:60000}")
    public void actualiser() {
        service.actualiserTousLesSoldes();
    }
}
