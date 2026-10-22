package com.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.entity.TypeQuestion;

public interface TypeQuestionRepository extends JpaRepository<TypeQuestion, Integer> {

    Optional<TypeQuestion> findByCode(String code);
}
