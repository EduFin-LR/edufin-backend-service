package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    // Para buscar todas las preguntas de una lección
    List<Question> findByLessonId(UUID lessonId);

    // ESTE ERA EL ANTIGUO RANDOM
    // Consulta nativa en PostgreSQL para traer N preguntas al azar
     @Query(value = "SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestions(@Param("limit") int limit);


    //ESTE ES EL NUEVO RANDOM
    // En QuestionRepository.java (Módulo Learning)
    @Query(value = "SELECT q.* FROM questions q " +
            "JOIN lessons l ON q.lesson_id = l.id " +
            "WHERE l.topic_id = :topicId " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestionsByTopic(@Param("topicId") UUID topicId, @Param("limit") int limit);
}
