package com.project.config;

import com.project.entity.TypeConge;
import com.project.repository.TypeCongeRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Garantit les types de conges de base sur une base existante. */
@Component
public class CongeReferenceInitializer implements CommandLineRunner {
    private final TypeCongeRepository types;

    public CongeReferenceInitializer(TypeCongeRepository types) {
        this.types = types;
    }

    @Override
    @Transactional
    public void run(String... args) {
        creer("CP", "Congés payés", true, true, new BigDecimal("2.50"));
        creer("RTT", "Réduction du temps de travail", true, true, new BigDecimal("1.00"));
        creer("MAL", "Arrêt maladie", true, false, null);
        creer("SANS_SOLDE", "Congé sans solde", false, false, null);
    }

    private void creer(String code, String libelle, boolean remunere, boolean decompte,
            BigDecimal acquisition) {
        if (types.findByCode(code).isPresent()) {
            return;
        }
        TypeConge type = new TypeConge();
        type.setCode(code);
        type.setLibelle(libelle);
        type.setRemunere(remunere);
        type.setDecompteSolde(decompte);
        type.setAcquisitionMensuelle(acquisition);
        type.setActif(true);
        types.save(type);
    }
}
