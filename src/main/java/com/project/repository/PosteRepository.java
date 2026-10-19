package com.project.repository;

import com.project.entity.Poste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PosteRepository extends JpaRepository<Poste, Integer> {
  List<Poste> findByActifTrueOrderByIntituleAsc();

  List<Poste> findAllByOrderByIntituleAsc();
}
