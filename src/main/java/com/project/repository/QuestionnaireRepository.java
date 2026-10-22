
package com.project.repository;

import java.util.Optional;
import java.util.List;

import com.project.entity.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Integer> {

    /** Questionnaire d'une offre (un seul par offre). */
    Optional<Questionnaire> findByOffre_Id(Integer idOffre);

    @Query("SELECT q FROM Questionnaire q JOIN FETCH q.offre")
    List<Questionnaire> findAllAvecOffre();
}
