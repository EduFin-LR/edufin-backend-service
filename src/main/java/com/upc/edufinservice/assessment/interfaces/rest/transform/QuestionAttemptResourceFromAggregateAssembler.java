package com.upc.edufinservice.assessment.interfaces.rest.transform;

import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.interfaces.rest.resources.QuestionAttemptResource;

public class QuestionAttemptResourceFromAggregateAssembler {
    public static QuestionAttemptResource toResourceFromAggregate(QuestionAttempt attempt) {
        return new QuestionAttemptResource(
                attempt.getId(),
                attempt.getUserId(),
                attempt.getQuestionId(),
                attempt.getSelectedOptionId(),
                attempt.getIsCorrect(),
                attempt.getTimeTakenSec(),
                attempt.getAttemptedAt()
        );
    }
}