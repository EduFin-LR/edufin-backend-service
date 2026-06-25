package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.UUID;

public record SubmitQuestionAttemptResource(
        UUID userId,
        UUID questionId,
        UUID selectedOptionId,
        Boolean isCorrect,
        Float timeTakenSec
) {}