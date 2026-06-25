package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record DiagnosticQuestionResource(
        UUID questionId,
        String questionText,
        List<DiagnosticOptionResource> options
) {}
