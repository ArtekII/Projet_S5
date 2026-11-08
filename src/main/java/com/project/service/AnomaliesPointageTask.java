package com.project.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pointage.detection-active", havingValue = "true", matchIfMissing = true)
public class AnomaliesPointageTask {
    private final PointageService service;
    public AnomaliesPointageTask(PointageService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${app.pointage.detection-intervalle-ms:60000}",
            initialDelayString = "${app.pointage.detection-delai-initial-ms:60000}")
    public void detecter() { service.detecterAnomalies(); }
}
