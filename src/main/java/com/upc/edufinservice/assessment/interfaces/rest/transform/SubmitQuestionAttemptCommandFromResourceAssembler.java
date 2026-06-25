package com.upc.edufinservice.assessment.interfaces.rest.transform;

import com.upc.edufinservice.assessment.domain.model.commands.SubmitQuestionAttemptCommand;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitQuestionAttemptResource;

public class SubmitQuestionAttemptCommandFromResourceAssembler {
    public static SubmitQuestionAttemptCommand toCommandFromResource(SubmitQuestionAttemptResource resource) {
        return new SubmitQuestionAttemptCommand(
                resource.userId(),
                resource.questionId(),
                resource.selectedOptionId(),
                resource.isCorrect(),
                resource.timeTakenSec()
        );
    }
}