package com.upc.edufinservice.assessment.interfaces.rest.resources;

public record ExperimentalAssessmentStatusResponse(
        boolean preTestCompleted,
        boolean postTestEligible,
        boolean postTestCompleted
) {}
