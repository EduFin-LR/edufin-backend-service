package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {

    // Estos métodos son para extraer data y mandarle a la IA engine
    List<QuestionAttempt> findByUserId(UUID userId);

    List<QuestionAttempt> findByUserIdAndQuestionId(UUID userId, UUID questionId);
}