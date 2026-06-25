package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.entities.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, UUID> {

    // ¡Aquí está la magia! Cambiamos ulp.lesson.topicId por ulp.lesson.topic.id
    @Query("SELECT COUNT(ulp) FROM UserLessonProgress ulp WHERE ulp.userId = :userId AND ulp.lesson.topic.id = :topicId AND ulp.completed = true")
    Integer countCompletedLessons(@Param("userId") UUID userId, @Param("topicId") UUID topicId);

    // 2. Busca el progreso específico de un alumno en una lección
    Optional<UserLessonProgress> findByUserIdAndLesson_Id(UUID userId, UUID lessonId);
}