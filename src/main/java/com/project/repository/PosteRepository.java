package com.project.repository;

import com.project.entity.Poste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosteRepository extends JpaRepository<Poste, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Poste p where p.id = :id")
  Optional<Poste> findByIdForUpdate(@Param("id") Integer id);

  List<Poste> findByActifTrueOrderByIntituleAsc();

  List<Poste> findAllByOrderByIntituleAsc();
}
