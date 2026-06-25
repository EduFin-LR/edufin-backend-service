package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record SubmitDiagnosticResource(
        UUID userId,
        List<DiagnosticAnswerResource> answers
) {}
