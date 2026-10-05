package com.upc.edufinservice.assessment.interfaces.rest.resources;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentPhase;

import java.time.Instant;
import java.util.UUID;

public record ExperimentalSubmissionResponse(
        UUID submissionId,
        ExperimentalAssessmentPhase phase,
        Integer totalQuestions,
        Instant submittedAt
) {}
