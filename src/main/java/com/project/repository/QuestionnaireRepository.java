
package com.project.repository;

import java.util.Optional;

import com.project.entity.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Integer> {

    /** Questionnaire d'une offre (un seul par offre). */
    Optional<Questionnaire> findByOffre_Id(Integer idOffre);
}
