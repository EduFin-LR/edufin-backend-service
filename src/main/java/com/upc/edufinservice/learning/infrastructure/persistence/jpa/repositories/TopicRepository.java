package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findByName(String name);

    // Devuelve los temas ordenados (ej. Topic 1 primero, Topic 2 después)
    List<Topic> findAllByOrderByTopicOrderAsc();
}
