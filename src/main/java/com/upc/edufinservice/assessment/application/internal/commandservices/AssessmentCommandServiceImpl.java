package com.upc.edufinservice.assessment.application.internal.commandservices;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.aggregates.UserLessonProgress;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.assessment.domain.model.commands.*;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredIncorrectlyEvent;
import com.upc.edufinservice.assessment.domain.services.AssessmentCommandService;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.queries.*;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssessmentCommandServiceImpl implements AssessmentCommandService {

    private final QuestionAttemptRepository _repository;
    private final UserLessonProgressRepository _userLessonProgressRepository;
    private final ApplicationEventPublisher _eventPublisher;
    private final LearningQueryService _learningQueryService;

    public AssessmentCommandServiceImpl(QuestionAttemptRepository repository,
                                        UserLessonProgressRepository userLessonProgressRepository,
                                        ApplicationEventPublisher eventPublisher,
                                        LearningQueryService learningQueryService){
        _repository = repository;
        _userLessonProgressRepository = userLessonProgressRepository;
        _eventPublisher = eventPublisher;
        _learningQueryService = learningQueryService;
    }

    @Override
    @Transactional
    public Optional<QuestionAttempt> handle(SubmitQuestionAttemptCommand command){
        boolean calculatedIsCorrect = false;

        var realOptions = _learningQueryService.handle(new GetOptionsByQuestionIdQuery(command.questionId()));

        for (var option : realOptions) {
            if (option.getId().equals(command.selectedOptionId())) {
                if (option.getMatchCategory() != null) {
                    if (command.selectedMatchCategory() != null &&
                            option.getMatchCategory().trim().equalsIgnoreCase(command.selectedMatchCategory().trim())) {
                        calculatedIsCorrect = true;
                    }
                }
                else {
                    if (Boolean.TRUE.equals(option.getIsCorrect())) {
                        calculatedIsCorrect = true;
                    }
                }
                break;
            }
        }

        var attempt = new QuestionAttempt(
                command.userId(),
                command.questionId(),
                command.selectedOptionId(),
                command.selectedMatchCategory(),
                calculatedIsCorrect,
                command.timeTakenSec(),
                command.interactionType(),
                command.selectionReason()
        );
        _repository.save(attempt);

        if (calculatedIsCorrect) {
            _eventPublisher.publishEvent(
                    new QuestionAnsweredCorrectlyEvent(
                            attempt.getUserId(),
                            attempt.getQuestionId(),
                            attempt.getInteractionType(),
                            attempt.getSelectionReason()
                    )
            );
        } else {
            _eventPublisher.publishEvent(
                    new QuestionAnsweredIncorrectlyEvent(
                            attempt.getUserId(),
                            attempt.getQuestionId(),
                            attempt.getInteractionType(),
                            attempt.getSelectionReason()
                    )
            );
        }

        return Optional.of(attempt);
    }

    // ========================================================================
    // 🔒 CONTROL DE INICIO CON VALIDACIÓN SECUENCIAL CRUZADA
    // ========================================================================
    @Override
    @Transactional
    public UserLessonProgress handle(StartLessonCommand command) {
        var progressOpt = _userLessonProgressRepository.findByUserIdAndLessonId(command.userId(), command.lessonId());
        // Escenario A: El registro ya existe en la Base de Datos (Fue desbloqueado previamente)
        if (progressOpt.isPresent()) {
            var progress = progressOpt.get();
            if (progress.getStatus() == ProgressStatus.LOCKED) {
                throw new IllegalStateException("No puedes iniciar esta lección porque se encuentra bloqueada en tu mapa.");
            }
            // Si estaba en UNLOCKED (esperando ser jugada), la marcamos oficialmente en curso
            if (progress.getStatus() == ProgressStatus.UNLOCKED) {
                progress.setStatus(ProgressStatus.IN_PROGRESS);
                _userLessonProgressRepository.save(progress);
            }
            return progress;
        }

        //CORRECCIÓN RELACIONAL: Buscamos el Topic navegando las colecciones sin depender de las preguntas
        Topic currentTopic = null;
        var allTopics = _learningQueryService.handle(new GetAllTopicsQuery());
        for (var t : allTopics) {
            var lessonsOfTopic = _learningQueryService.handle(new GetLessonsByTopicIdQuery(t.getId()));
            if (lessonsOfTopic.stream().anyMatch(l -> l.getId().equals(command.lessonId()))) {
                currentTopic = t;
                break;
            }
        }

        if (currentTopic == null) {
            throw new IllegalArgumentException("La lección solicitada no es válida o carece de contexto pedagógico.");
        }

        var orderedLessons = _learningQueryService.handle(new GetLessonsByTopicIdQuery(currentTopic.getId()));

        boolean isFirstLessonOfTopic = !orderedLessons.isEmpty() && orderedLessons.get(0).getId().equals(command.lessonId());
        boolean isFirstTopicOfApp = !allTopics.isEmpty() && allTopics.get(0).getId().equals(currentTopic.getId());

        if (isFirstLessonOfTopic && isFirstTopicOfApp) {
            UserLessonProgress initialProgress = new UserLessonProgress(command.userId(), command.lessonId());
            initialProgress.setStatus(ProgressStatus.IN_PROGRESS);
            return _userLessonProgressRepository.save(initialProgress);
        }

        // Si intentó forzar el inicio de cualquier otro nivel sin tener un registro previo en UNLOCKED, se rechaza
        throw new IllegalStateException("Acceso Denegado: Debes completar las lecciones predecesoras antes de acceder a este nivel.");
    }


    // ========================================================================
    // COMPLETADO DE LECCIÓN
    // ========================================================================
    @Override
    @Transactional
    public LessonCompletionResponse handle(CompleteLessonCommand command) {
        var progress = _userLessonProgressRepository
                .findByUserIdAndLessonId(command.userId(), command.lessonId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un progreso activo para esta lección."));

        // El frontend envía exactamente las preguntas que fueron presentadas
        // en esta ejecución (8+2 en quiz adaptativo, por ejemplo). Así evitamos
        // comparar contra todo el banco de preguntas de la lección.
        var presentedQuestionIds = new LinkedHashSet<>(command.questionIds());

        int totalQuestions = presentedQuestionIds.size();
        int correctQuestions = 0;
        int totalAtomicAttempts = 0;
        int correctAtomicAttempts = 0;

        for (UUID questionId : presentedQuestionIds) {
            var question = _learningQueryService
                    .handle(new GetQuestionByIdQuery(questionId))
                    .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada: " + questionId));

            var quizAttempts = _repository
                    .findByUserIdAndQuestionId(command.userId(), questionId)
                    .stream()
                    .filter(this::isQuizAttempt)
                    .sorted(Comparator.comparing(QuestionAttempt::getAttemptedAt).reversed())
                    .toList();

            if (quizAttempts.isEmpty()) {
                continue;
            }

            boolean questionCorrect;

            if ("DRAG_AND_DROP".equalsIgnoreCase(question.getQuestionType())) {
                // Una pregunta drag & drop genera un attempt por tarjeta/opción.
                // Para la nota del quiz, sin embargo, la pregunta visual cuenta una
                // sola vez. Tomamos el attempt más reciente de cada opción y exigimos
                // que estén todas las opciones y todas sean correctas.
                var expectedOptions = _learningQueryService.handle(
                        new GetOptionsByQuestionIdQuery(questionId)
                );

                Map<UUID, QuestionAttempt> latestByOption = quizAttempts.stream()
                        .filter(a -> a.getSelectedOptionId() != null)
                        .collect(Collectors.toMap(
                                QuestionAttempt::getSelectedOptionId,
                                Function.identity(),
                                (newer, older) -> newer,
                                LinkedHashMap::new
                        ));

                totalAtomicAttempts += latestByOption.size();
                correctAtomicAttempts += (int) latestByOption.values().stream()
                        .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                        .count();

                questionCorrect = !expectedOptions.isEmpty()
                        && latestByOption.size() == expectedOptions.size()
                        && expectedOptions.stream().allMatch(option -> {
                    var attempt = latestByOption.get(option.getId());
                    return attempt != null && Boolean.TRUE.equals(attempt.getIsCorrect());
                });
            } else {
                // Multiple choice: solo importa el intento QUIZ/REINFORCEMENT
                // más reciente de esa pregunta, no un acierto histórico.
                var latestAttempt = quizAttempts.get(0);
                totalAtomicAttempts += 1;
                questionCorrect = Boolean.TRUE.equals(latestAttempt.getIsCorrect());
                if (questionCorrect) {
                    correctAtomicAttempts++;
                }
            }

            if (questionCorrect) {
                correctQuestions++;
            }
        }

        int incorrectQuestions = totalQuestions - correctQuestions;
        float calculatedScore = totalQuestions > 0
                ? ((float) correctQuestions / totalQuestions) * 100
                : 100.0f;

        progress.markAsCompleted(calculatedScore, command.timeSpentSec());
        _userLessonProgressRepository.save(progress);

        // Buscador macro para identificar el tema actual
        Topic currentTopic = null;
        var allTopics = _learningQueryService.handle(new GetAllTopicsQuery());
        for (var t : allTopics) {
            var lessonsOfTopic = _learningQueryService.handle(new GetLessonsByTopicIdQuery(t.getId()));
            if (lessonsOfTopic.stream().anyMatch(l -> l.getId().equals(command.lessonId()))) {
                currentTopic = t;
                break;
            }
        }

        if (currentTopic != null) {
            var orderedLessons = _learningQueryService.handle(new GetLessonsByTopicIdQuery(currentTopic.getId()));

            for (int i = 0; i < orderedLessons.size(); i++) {
                if (orderedLessons.get(i).getId().equals(command.lessonId())) {
                    if (i + 1 < orderedLessons.size()) {
                        var nextLesson = orderedLessons.get(i + 1);
                        var nextProgress = _userLessonProgressRepository
                                .findByUserIdAndLessonId(command.userId(), nextLesson.getId())
                                .orElseGet(() -> {
                                    UserLessonProgress ulp = new UserLessonProgress(command.userId(), nextLesson.getId());
                                    ulp.setStatus(ProgressStatus.LOCKED);
                                    return ulp;
                                });

                        if (nextProgress.getStatus() == ProgressStatus.LOCKED) {
                            nextProgress.setStatus(ProgressStatus.UNLOCKED);
                            _userLessonProgressRepository.save(nextProgress);
                        }
                    } else {
                        System.out.println(
                                "[FINAL] Última lección del módulo completada. "
                                        + "La evaluación final ya puede ser presentada."
                        );
                    }
                    break;
                }
            }
        }

        int experienceGainedForCompletion = Math.round(calculatedScore);
        // Los eventos de respuesta correcta otorgan 10 XP por interacción atómica.
        // En drag & drop cada tarjeta es una interacción, por eso este valor se
        // calcula a nivel de opción, mientras que la nota sigue siendo por pregunta.
        int experienceFromQuestions = correctAtomicAttempts * 10;
        int totalExperience = experienceGainedForCompletion + experienceFromQuestions;

        _eventPublisher.publishEvent(new com.upc.edufinservice.assessment.domain.model.events.LessonCompletedEvent(
                command.userId(),
                command.lessonId(),
                calculatedScore,
                totalAtomicAttempts
        ));

        return new LessonCompletionResponse(
                totalQuestions,
                correctQuestions,
                incorrectQuestions,
                experienceGainedForCompletion,
                experienceFromQuestions,
                totalExperience
        );
    }

    private boolean isQuizAttempt(QuestionAttempt attempt) {
        return attempt.getInteractionType() == InteractionType.QUIZ
                || attempt.getInteractionType() == InteractionType.REINFORCEMENT;
    }

}