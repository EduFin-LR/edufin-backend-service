package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    // Para buscar todas las preguntas de una lección
    List<Question> findByLessonId(UUID lessonId);
}
