
package com.project.repository;

import java.util.List;

import com.project.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    /** Questions d'un questionnaire avec leurs choix, dans l'ordre d'affichage. */
    @Query("""
        SELECT DISTINCT q
        FROM Question q
        LEFT JOIN FETCH q.choix
        JOIN FETCH q.type
        WHERE q.questionnaire.id = :idQuestionnaire
        ORDER BY q.ordre
    """)
    List<Question> findByQuestionnaireIdAvecChoix(
            @Param("idQuestionnaire") Integer idQuestionnaire);
}
