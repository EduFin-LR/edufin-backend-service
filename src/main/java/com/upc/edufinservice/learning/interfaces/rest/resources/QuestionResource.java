package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record QuestionResource(
        UUID id,
        String questionText,
        String explanation,
        String questionType,       // Nuevo
        String hint,               // Nuevo
        String successMessage,     // Nuevo
        String errorMessage,       // Nuevo
        List<QuestionOptionResource> options) {}