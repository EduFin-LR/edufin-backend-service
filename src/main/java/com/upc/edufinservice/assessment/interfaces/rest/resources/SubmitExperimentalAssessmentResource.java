package com.upc.edufinservice.assessment.interfaces.rest.resources;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentPhase;

import java.util.List;

public record SubmitExperimentalAssessmentResource(
        ExperimentalAssessmentPhase phase,
        List<ExperimentalAnswerResource> answers
) {}
