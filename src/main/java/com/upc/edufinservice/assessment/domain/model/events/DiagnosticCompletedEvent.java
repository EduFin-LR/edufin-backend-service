package com.upc.edufinservice.assessment.domain.model.events;

import java.util.UUID;

public record DiagnosticCompletedEvent(
        UUID userId,
        Float score
) {
}
