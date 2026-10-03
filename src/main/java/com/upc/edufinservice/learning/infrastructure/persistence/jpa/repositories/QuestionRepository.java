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


    // NUEVA CONSULTA: Extrae preguntas aleatorias de reforzamiento para la Side Quest
    // Filtra por la habilidad en crisis (dktSkillId) y el tipo de lección evaluativa (ej. 'QUIZ')
    @Query(value = "SELECT q.* FROM questions q " +
            "JOIN lessons l ON q.lesson_id = l.id " +
            "WHERE q.skill_id = :dktSkillId " +
            "AND l.lesson_type = :lessonType " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findSideQuestQuestionsBySkillAndType(
            @Param("dktSkillId") Integer dktSkillId,
            @Param("lessonType") String lessonType,
            @Param("limit") int limit
    );

    /**
     * Consulta genérica para obtener preguntas QUIZ de una skill concreta.
     * Usa questions.skill_id, que es la relación actual Question -> Skill.
     *
     * Esta consulta será la base para selección por bajo mastery.
     */
    @Query(value = "SELECT q.* FROM questions q " +
            "JOIN lessons l ON q.lesson_id = l.id " +
            "WHERE q.skill_id = :skillId " +
            "AND l.lesson_type = 'QUIZ' " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuizQuestionsBySkill(
            @Param("skillId") Integer skillId,
            @Param("limit") int limit
    );


    /**
     * Obtiene solamente N preguntas aleatorias de una lección.
     * La limitación se hace directamente en PostgreSQL.
     */
    @Query(value = "SELECT q.* FROM questions q " +
            "WHERE q.lesson_id = :lessonId " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestionsByLessonId(
            @Param("lessonId") UUID lessonId,
            @Param("limit") int limit
    );


    /**
     * Obtiene preguntas solamente de bancos QUIZ pertenecientes a un Topic/módulo.
     * Se usa para construir el FINAL dinámico sin depender de filas FINAL persistidas.
     */
    @Query(value = "SELECT q.* FROM questions q " +
            "JOIN lessons l ON q.lesson_id = l.id " +
            "WHERE l.topic_id = :topicId " +
            "AND l.lesson_type = 'QUIZ' " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuizQuestionsByTopic(
            @Param("topicId") UUID topicId,
            @Param("limit") int limit
    );

}
