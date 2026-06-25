package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {
    //Para busccar todas las opciones de una pregunta especifica
    List<QuestionOption> findByQuestionId(UUID questionId);
}
