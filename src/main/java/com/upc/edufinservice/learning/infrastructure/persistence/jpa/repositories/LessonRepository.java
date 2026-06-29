package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    // 1. Agregamos el guion bajo (_) para que busque por el ID del objeto Topic
    List<Lesson> findByTopic_Id(UUID topicId);

    Optional<Lesson> findByTitleAndTopicId(String title, UUID topicID);

    // 2. Cambiamos l.topicId por l.topic.id
    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.topic.id = :topicId")
    Integer countByTopicId(@Param("topicId") UUID topicId);

    // Devuelve las lecciones ordenadas
    List<Lesson> findByTopic_IdOrderByLessonOrderAsc(UUID topicId);

    Optional<Lesson> findByTopic_IdAndLessonOrder(UUID topicId, Integer lessonOrder);
}
