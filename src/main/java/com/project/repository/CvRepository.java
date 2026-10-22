
package com.project.repository;

import java.util.List;

import com.project.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CvRepository extends JpaRepository<Cv, Integer> {

    List<Cv> findByCandidat_Id(Integer idCandidat);
}
