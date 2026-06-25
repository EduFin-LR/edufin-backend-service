package com.upc.edufinservice.assessment.application.internal.commandservices;

import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.commands.SubmitQuestionAttemptCommand;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.domain.services.AssessmentCommandService;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AssessmentCommandServiceImpl implements AssessmentCommandService {
    private final QuestionAttemptRepository _repository;
    private final ApplicationEventPublisher _eventPublisher;

    public AssessmentCommandServiceImpl(QuestionAttemptRepository repository, ApplicationEventPublisher eventPublisher){
        _repository  = repository;
        _eventPublisher = eventPublisher;
    }

    @Override
    public Optional<QuestionAttempt> handle(SubmitQuestionAttemptCommand command){
        // 1. Creamos y guardamos el intento en la base de datos
        var attempt = new QuestionAttempt(
                command.userId(),
                command.questionId(),
                command.selectedOptionId(),
                command.isCorrect(),
                command.timeTakenSec()
        );
        _repository.save(attempt);

        // 2. Si la respuesta fue correcta, disparamos el evento para que Gamificación lo escuche
        if (Boolean.TRUE.equals(attempt.getIsCorrect())) {
            _eventPublisher.publishEvent(new QuestionAnsweredCorrectlyEvent(attempt.getUserId()));
        }

        return Optional.of(attempt);
    }
}
