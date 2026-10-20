package com.project.service;

import com.project.entity.CvCandidat;
import com.project.entity.OffreEmploi;
import com.project.entity.Utilisateur;
import com.project.repository.CvCandidatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CandidatureService {
    private final CvCandidatRepository cvRepository;
    private final OffreService offreService;

    public CandidatureService(CvCandidatRepository cvRepository, OffreService offreService) {
        this.cvRepository = cvRepository;
        this.offreService = offreService;
    }

    public boolean aDejaCandidate(Integer candidatId, Integer offreId) {
        return cvRepository.existsByCandidat_IdAndOffre_Id(candidatId, offreId);
    }

    @Transactional
    public CvCandidat enregistrer(Integer offreId, Utilisateur candidat, String nom, String prenom,
            LocalDate dateNaissance, Integer anneeExperience) {
        OffreEmploi offre = offreService.findOffre(offreId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable."));
        if (!"PUBLIEE".equals(offre.getStatut().getCode())) {
            throw new IllegalStateException("Cette offre n'est plus disponible.");
        }
        if (cvRepository.existsByCandidat_IdAndOffre_Id(candidat.getId(), offreId)) {
            throw new IllegalStateException("Vous avez déjà candidaté à cette offre.");
        }

        CvCandidat cv = new CvCandidat();
        cv.setCandidat(candidat);
        cv.setOffre(offre);
        cv.setNom(nom.trim());
        cv.setPrenom(prenom.trim());
        cv.setDateNaissance(dateNaissance);
        cv.setAnneeExperience(anneeExperience);
        cv.setEmail(candidat.getEmail());
        cv.setCreatedAt(LocalDateTime.now());
        return cvRepository.save(cv);
    }
}
