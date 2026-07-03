package com.upc.edufinservice.assessment.interfaces.rest.resources;

import com.upc.edufinservice.learning.interfaces.rest.resources.QuestionResource;

import java.util.List;

public record SideQuestResource(
        boolean isSideQuestActive,
        String message,
        List<QuestionResource> questions
) {
}
