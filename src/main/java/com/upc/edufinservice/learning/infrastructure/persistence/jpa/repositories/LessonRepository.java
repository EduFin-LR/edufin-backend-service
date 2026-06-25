package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByTopicId(UUID topicId);
}
