package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record ExperimentalQuestionResource(
        UUID id,
        String code,
        Integer questionOrder,
        Integer moduleNumber,
        String moduleName,
        String competency,
        String questionText,
        List<ExperimentalOptionResource> options
) {}
