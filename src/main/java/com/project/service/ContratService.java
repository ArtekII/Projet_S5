package com.project.service;

import org.springframework.stereotype.Service;
import com.project.entity.*;
import com.project.repository.*;

@Service 
public class ContratService {
    private final ContratRepository contratRepo;
    private final TypeContratRepository typeContratRepo;
    private final ConditionTravailRepository conditionRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final ReponseConditionRepository reponseConditionRepo;

    public ContratService(ContratRepository c, TypeContratRepository t, ConditionTravailRepository cond, UtilisateurRepository u, ReponseConditionRepository r) {
        this.contratRepo = c;
        this.typeContratRepo = t;
        this.conditionRepo = cond;
        this.utilisateurRepo = u;
        this.reponseConditionRepo = r;
    }

    
    
}
