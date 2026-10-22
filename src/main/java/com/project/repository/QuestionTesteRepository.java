package com.project.repository;

import com.project.entity.QuestionTeste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionTesteRepository extends JpaRepository<QuestionTeste, Integer> {
    List<QuestionTeste> findByOffre_IdOrderByIdAsc(Integer offreId);
}
