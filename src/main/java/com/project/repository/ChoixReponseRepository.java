
package com.project.repository;

import java.util.List;

import com.project.entity.ChoixReponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChoixReponseRepository extends JpaRepository<ChoixReponse, Integer> {

    List<ChoixReponse> findByQuestion_IdOrderByIdAsc(Integer idQuestion);
}
