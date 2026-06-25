package com.upc.edufinservice.assessment.domain.services;

import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.commands.SubmitQuestionAttemptCommand;

import java.util.Optional;

public interface AssessmentCommandService {
    Optional<QuestionAttempt> handle(SubmitQuestionAttemptCommand command);
}