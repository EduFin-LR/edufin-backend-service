package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.UUID;

public record QuestionOptionResource(UUID id, String optionText, Boolean isCorrect) {}
