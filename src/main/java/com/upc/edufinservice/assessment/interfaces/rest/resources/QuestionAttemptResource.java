package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuestionAttemptResource(
        UUID id,
        UUID userId,
        UUID questionId,
        UUID selectedOptionId,
        String selectedMatchCategory,
        Boolean isCorrect,
        Float timeTakenSec,
        LocalDateTime attemptedAt
) {}