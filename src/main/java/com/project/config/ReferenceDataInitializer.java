package com.project.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.Etape;
import com.project.entity.EtapeRecrutement;
import com.project.entity.Resultat;
import com.project.entity.StatutCandidature;
import com.project.entity.TypeQuestion;
import com.project.repository.EtapeRecrutementRepository;
import com.project.repository.EtapeRepository;
import com.project.repository.ResultatRepository;
import com.project.repository.StatutCandidatureRepository;
import com.project.repository.TypeQuestionRepository;

/**
 * Garantit les donnees de reference necessaires au parcours candidat/RH sur
 * une base PostgreSQL locale deja existante.
 */
@Component
public class ReferenceDataInitializer implements CommandLineRunner {

    private final EtapeRecrutementRepository etapeRecrutementRepository;
    private final StatutCandidatureRepository statutCandidatureRepository;
    private final ResultatRepository resultatRepository;
    private final EtapeRepository etapeRepository;
    private final TypeQuestionRepository typeQuestionRepository;

    public ReferenceDataInitializer(
            EtapeRecrutementRepository etapeRecrutementRepository,
            StatutCandidatureRepository statutCandidatureRepository,
            ResultatRepository resultatRepository,
            EtapeRepository etapeRepository,
            TypeQuestionRepository typeQuestionRepository) {
        this.etapeRecrutementRepository = etapeRecrutementRepository;
        this.statutCandidatureRepository = statutCandidatureRepository;
        this.resultatRepository = resultatRepository;
        this.etapeRepository = etapeRepository;
        this.typeQuestionRepository = typeQuestionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureEtapesRecrutement();
        ensureStatuts();
        ensureResultats();
        ensureEtapesRh();
        ensureTypesQuestion();
    }

    private void ensureEtapesRecrutement() {
        List<String> codes = List.of("CV", "QUESTIONNAIRE", "ENTRETIEN",
                "CONDITIONS", "EMBAUCHE");
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            if (etapeRecrutementRepository.findByCode(code).isEmpty()) {
                EtapeRecrutement etape = new EtapeRecrutement();
                etape.setCode(code);
                etape.setLibelle(libelleEtape(code));
                etape.setOrdre(i + 1);
                etapeRecrutementRepository.save(etape);
            }
        }
    }

    private void ensureStatuts() {
        ensureStatut("EN_COURS", "En cours");
        ensureStatut("ELIMINE", "Elimine");
        ensureStatut("REFUSE_PAR_CANDIDAT", "Refuse par le candidat");
        ensureStatut("EMBAUCHE", "Embauche");
    }

    private void ensureStatut(String code, String libelle) {
        if (statutCandidatureRepository.findByCode(code).isEmpty()) {
            StatutCandidature statut = new StatutCandidature();
            statut.setCode(code);
            statut.setLibelle(libelle);
            statutCandidatureRepository.save(statut);
        }
    }

    private void ensureResultats() {
        ensureResultat("REUSSI", "Reussi");
        ensureResultat("ECHOUE", "Echoue");
    }

    private void ensureResultat(String code, String libelle) {
        if (resultatRepository.findByCode(code).isEmpty()) {
            Resultat resultat = new Resultat();
            resultat.setCode(code);
            resultat.setLibelle(libelle);
            resultatRepository.save(resultat);
        }
    }

    private void ensureEtapesRh() {
        for (String nom : List.of("selection", "Test", "Entretien")) {
            if (etapeRepository.findByNom(nom).isEmpty()) {
                etapeRepository.save(new Etape(nom));
            }
        }
    }

    private void ensureTypesQuestion() {
        ensureTypeQuestion("QCM", "Choix multiple");
        ensureTypeQuestion("TEXTE", "Réponse libre");
    }

    private void ensureTypeQuestion(String code, String libelle) {
        if (typeQuestionRepository.findByCode(code).isEmpty()) {
            TypeQuestion type = new TypeQuestion();
            type.setCode(code);
            type.setLibelle(libelle);
            typeQuestionRepository.save(type);
        }
    }

    private String libelleEtape(String code) {
        return switch (code) {
            case "CV" -> "Selection des CV";
            case "QUESTIONNAIRE" -> "Questionnaire";
            case "ENTRETIEN" -> "Entretien";
            case "CONDITIONS" -> "Conditions de travail";
            case "EMBAUCHE" -> "Embauche";
            default -> code;
        };
    }
}
