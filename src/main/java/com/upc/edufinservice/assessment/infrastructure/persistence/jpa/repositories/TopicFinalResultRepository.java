package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.aggregates.TopicFinalResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicFinalResultRepository
        extends JpaRepository<TopicFinalResult, UUID> {

    Optional<TopicFinalResult> findByUserIdAndTopicId(
            UUID userId,
            UUID topicId
    );

    boolean existsByUserIdAndTopicIdAndPassedTrue(
            UUID userId,
            UUID topicId
    );
}
