package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.UUID;

public record ExperimentalAnswerResource(UUID questionId, UUID selectedOptionId, Float timeTakenSec) {}
