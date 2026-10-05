package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentalQuestionRepository extends JpaRepository<ExperimentalQuestion, UUID> {
    Optional<ExperimentalQuestion> findByCode(String code);
    List<ExperimentalQuestion> findAllByOrderByQuestionOrderAsc();
}
