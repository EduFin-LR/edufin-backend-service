package com.upc.edufinservice.assessment.interfaces.rest.transform;

import com.upc.edufinservice.assessment.domain.model.commands.SubmitQuestionAttemptCommand;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitQuestionAttemptResource;

import java.util.UUID;

public class SubmitQuestionAttemptCommandFromResourceAssembler {

    public static SubmitQuestionAttemptCommand toCommandFromResource(
            SubmitQuestionAttemptResource resource,
            UUID safeUserId) {

        return new SubmitQuestionAttemptCommand(
                safeUserId,
                resource.questionId(),
                resource.selectedOptionId(),
                resource.selectedMatchCategory(), // ¡Inyectado para Drag & Drop!
                resource.timeTakenSec()
        );
    }
}