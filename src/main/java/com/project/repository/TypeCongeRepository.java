package com.project.repository;

import com.project.entity.TypeConge;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TypeCongeRepository extends JpaRepository<TypeConge, Integer> {
    Optional<TypeConge> findByCode(String code);

    List<TypeConge> findByActifTrueOrderByIdAsc();
}
