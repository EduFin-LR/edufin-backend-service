package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExperimentalAssessmentResponseRepository extends JpaRepository<ExperimentalAssessmentResponse, UUID> {
}
