package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record SubmitDiagnosticResource(
        List<DiagnosticAnswerResource> answers
) {}
