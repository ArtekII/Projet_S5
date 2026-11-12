package com.project.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Rend effectifs les contrats programmes a leur date de commencement. */
@Component
public class ContratEffectifTask {
    private final ContratEmployeService service;

    public ContratEffectifTask(ContratEmployeService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.contrats.activation-intervalle-ms:60000}",
            initialDelayString = "${app.contrats.activation-delai-initial-ms:10000}")
    public void activer() {
        service.activerContratsDus();
    }
}
