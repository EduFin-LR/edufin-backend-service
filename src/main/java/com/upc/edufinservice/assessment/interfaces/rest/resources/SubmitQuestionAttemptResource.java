package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.UUID;

public record SubmitQuestionAttemptResource(
        UUID questionId,
        UUID selectedOptionId,
        Float timeTakenSec,
        String selectedMatchCategory
) {}