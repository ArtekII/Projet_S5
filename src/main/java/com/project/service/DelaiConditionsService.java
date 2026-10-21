package com.project.service;

import com.project.entity.ConditionTravail;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DelaiConditionsService {
    private final Clock clock;
    private final int jours;

    public DelaiConditionsService(Clock clock, @Value("${app.conditions.delai-jours:7}") int jours) {
        if (jours < 1) throw new IllegalArgumentException("Le delai doit etre positif");
        this.clock = clock;
        this.jours = jours;
    }

    public LocalDateTime maintenant() { return LocalDateTime.now(clock); }
    public LocalDateTime prolonger(LocalDateTime base) { return base.plusDays(jours); }
    public LocalDateTime nouvelleEcheance() { return maintenant().plusDays(jours); }

    public boolean expire(ConditionTravail condition) {
        return condition.getRepondreAvant() != null && !maintenant().isBefore(condition.getRepondreAvant());
    }

    public void verifier(ConditionTravail condition) {
        if (condition.getRepondreAvant() == null || expire(condition)) {
            throw new IllegalStateException("Le delai de reponse est expire. Contactez le RH pour une prolongation.");
        }
    }
}
