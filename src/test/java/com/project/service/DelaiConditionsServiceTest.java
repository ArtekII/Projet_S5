package com.project.service;

import com.project.entity.ConditionTravail;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DelaiConditionsServiceTest {
    private final DelaiConditionsService service = new DelaiConditionsService(
            Clock.fixed(Instant.parse("2026-10-21T09:00:00Z"), ZoneId.of("Indian/Antananarivo")), 7);

    @Test
    void echeanceExacteEstExpireeMaisPasLaSecondePrecedente() {
        ConditionTravail condition = new ConditionTravail();
        condition.setRepondreAvant(LocalDateTime.of(2026, 10, 21, 12, 0));
        assertTrue(service.expire(condition));
        assertThrows(IllegalStateException.class, () -> service.verifier(condition));
        condition.setRepondreAvant(condition.getRepondreAvant().plusSeconds(1));
        assertDoesNotThrow(() -> service.verifier(condition));
        assertEquals(LocalDateTime.of(2026, 10, 28, 12, 0), service.nouvelleEcheance());
    }
}
