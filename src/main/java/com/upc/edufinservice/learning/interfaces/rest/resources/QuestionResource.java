package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record QuestionResource(
        UUID id,
        String questionText,
        String explanation,
        String questionType,
        String hint,
        String successMessage,
        String errorMessage,
        Integer dktSkillId,
        String TheoryText,
        List<QuestionOptionResource> options) {}