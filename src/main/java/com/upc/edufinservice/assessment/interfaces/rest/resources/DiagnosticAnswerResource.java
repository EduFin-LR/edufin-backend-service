package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.UUID;

public record DiagnosticAnswerResource(
        UUID questionId,
        UUID selectedOptionId,
        Float timeTakenSec // Opcional, pero vital para tu modelo DKT en el futuro
) {}
