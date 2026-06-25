package com.upc.edufinservice.assessment.application.internal.commandservices;

import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.commands.SubmitQuestionAttemptCommand;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredIncorrectlyEvent;
import com.upc.edufinservice.assessment.domain.services.AssessmentCommandService;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AssessmentCommandServiceImpl implements AssessmentCommandService {
    private final QuestionAttemptRepository _repository;
    private final ApplicationEventPublisher _eventPublisher;
    private final LearningQueryService _learningQueryService;

    public AssessmentCommandServiceImpl(QuestionAttemptRepository repository,
                                        ApplicationEventPublisher eventPublisher,
                                        LearningQueryService learningQueryService){
        _repository  = repository;
        _eventPublisher = eventPublisher;
        _learningQueryService = learningQueryService;
    }

    @Override
    public Optional<QuestionAttempt> handle(SubmitQuestionAttemptCommand command){
        // 3. LA MAGIA DE LA SEGURIDAD: Verificamos por nosotros mismos si es correcta
        boolean calculatedIsCorrect = false;
        var realOptions = _learningQueryService.handle(new GetOptionsByQuestionIdQuery(command.questionId()));

        for (var option : realOptions) {
            // Si el ID de la opción coincide con la que marcó el usuario Y además es la correcta
            if (option.getId().equals(command.selectedOptionId()) && option.getIsCorrect()) {
                calculatedIsCorrect = true;
                break;
            }
        }

        // 4. Creamos el intento usando NUESTRO cálculo, no confiamos en el exterior
        var attempt = new QuestionAttempt(
                command.userId(),
                command.questionId(),
                command.selectedOptionId(),
                calculatedIsCorrect, // Usamos la variable segura
                command.timeTakenSec()
        );
        _repository.save(attempt);

        // 5. Disparamos el evento para que Gamificación lo escuche
        if (calculatedIsCorrect) {
            _eventPublisher.publishEvent(new QuestionAnsweredCorrectlyEvent(attempt.getUserId(), attempt.getQuestionId()));
        } else {
            _eventPublisher.publishEvent(new QuestionAnsweredIncorrectlyEvent(attempt.getUserId(), attempt.getQuestionId()));
        }

        return Optional.of(attempt);
    }
}
