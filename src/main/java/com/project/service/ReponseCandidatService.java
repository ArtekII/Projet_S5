package com.project.service;
import org.springframework.stereotype.Service;

import com.project.repository.ReponseCandidatRepository;
import com.project.entity.Candidature;
import com.project.entity.ReponseCandidat;
import com.project.repository.CandidatureRepository;
import java.util.List;

@Service 
public class ReponseCandidatService {

    private final ReponseCandidatRepository reponseCandidatRepository;
    private final CandidatureRepository candidatureRepository;

    public ReponseCandidatService(ReponseCandidatRepository reponseCandidatRepository, CandidatureRepository candidatureRepository) {
        this.reponseCandidatRepository = reponseCandidatRepository;
        this.candidatureRepository = candidatureRepository;
    }

    public List<ReponseCandidat> getReponsesByCandidatureId(Integer idCandidature) {
        return reponseCandidatRepository.findByCandidature_Id(idCandidature);
    }

    public boolean aReponduQuestionnaire(Integer idCandidature) {
        Candidature candidature = candidatureRepository.findById(idCandidature)
                .orElseThrow(() -> new IllegalStateException("Candidature introuvable."));
        return candidature.getScoreQuestionnaire() != null;
    }

}