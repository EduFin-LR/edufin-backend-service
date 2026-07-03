package com.upc.edufinservice.assessment.application.internal.commandservices;

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

import java.util.Optional;

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
                command.timeTakenSec()
        );
        _repository.save(attempt);

        if (calculatedIsCorrect) {
            _eventPublisher.publishEvent(new QuestionAnsweredCorrectlyEvent(attempt.getUserId(), attempt.getQuestionId()));
        } else {
            _eventPublisher.publishEvent(new QuestionAnsweredIncorrectlyEvent(attempt.getUserId(), attempt.getQuestionId()));
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
    // COMPLETADO DE LECCIÓN (FIXED FOR VIDEOS & MULTI-QUESTIONS)
    // ========================================================================
    @Override
    @Transactional
    public LessonCompletionResponse handle(CompleteLessonCommand command) {
        var progress = _userLessonProgressRepository
                .findByUserIdAndLessonId(command.userId(), command.lessonId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un progreso activo para esta lección."));

        var lessonQuestions = _learningQueryService.handle(new GetQuestionsByLessonIdQuery(command.lessonId()));
        int totalQuestions = lessonQuestions.size();
        int correctQuestions = 0;
        int totalAttempts = 0;

        for (var question : lessonQuestions) {
            var attempts = _repository.findByUserIdAndQuestionId(command.userId(), question.getId());
            totalAttempts += attempts.size();
            boolean hasCorrectAttempt = attempts.stream().anyMatch(QuestionAttempt::getIsCorrect);
            if (hasCorrectAttempt) {
                correctQuestions++;
            }
        }

        int incorrectQuestions = totalQuestions - correctQuestions;
        float calculatedScore = totalQuestions > 0 ? ((float) correctQuestions / totalQuestions) * 100 : 100.0f;

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
                        // 🟢 ESCENARIO A: Hay una siguiente lección dentro del MISMO Tema
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
                        // 🔥 ESCENARIO B: ¡NUEVA LÓGICA! Era la ÚLTIMA lección del tema actual. Transición al Siguiente Tema.
                        for (int j = 0; j < allTopics.size(); j++) {
                            if (allTopics.get(j).getId().equals(currentTopic.getId())) {
                                if (j + 1 < allTopics.size()) {
                                    // Existe un siguiente módulo en el mapa (Ej: pasar de Tema 1 a Tema 2)
                                    var nextTopic = allTopics.get(j + 1);
                                    var nextTopicLessons = _learningQueryService.handle(new GetLessonsByTopicIdQuery(nextTopic.getId()));

                                    if (!nextTopicLessons.isEmpty()) {
                                        // Extraemos de forma segura la PRIMERÍSIMA lección del nuevo módulo
                                        var firstLessonOfNextTopic = nextTopicLessons.get(0);

                                        var nextTopicLessonProgress = _userLessonProgressRepository
                                                .findByUserIdAndLessonId(command.userId(), firstLessonOfNextTopic.getId())
                                                .orElseGet(() -> {
                                                    UserLessonProgress ulp = new UserLessonProgress(command.userId(), firstLessonOfNextTopic.getId());
                                                    ulp.setStatus(ProgressStatus.LOCKED);
                                                    return ulp;
                                                });

                                        if (nextTopicLessonProgress.getStatus() == ProgressStatus.LOCKED) {
                                            nextTopicLessonProgress.setStatus(ProgressStatus.UNLOCKED);
                                            _userLessonProgressRepository.save(nextTopicLessonProgress);
                                            System.out.println("🔓 [TRACKING] ¡Tema completado con éxito! Desbloqueada la lección inicial del siguiente tema: " + firstLessonOfNextTopic.getTitle());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }

        // ========================================================================
        // 🎯 CÁLCULO DE XP Y PUBLICACIÓN DEL EVENTO
        // ========================================================================

        // 1. Bono por completar la lección (Igual a tu Math.round)
        int experienceGainedForCompletion = Math.round(calculatedScore);

        // 2. Puntos que el alumno YA fue ganando por sus respuestas (10 XP por cada buena)
        int experienceFromQuestions = correctQuestions * 10;

        // 3. Suma total para la celebración final
        int totalExperience = experienceGainedForCompletion + experienceFromQuestions;

        // Disparamos el evento original para que Gamificación guarde el bono final
        _eventPublisher.publishEvent(new com.upc.edufinservice.assessment.domain.model.events.LessonCompletedEvent(
                command.userId(),
                command.lessonId(),
                calculatedScore,
                totalAttempts
        ));

        // Retornamos el DTO desglosado hacia el controlador
        return new LessonCompletionResponse(
                totalQuestions,
                correctQuestions,
                incorrectQuestions,
                experienceGainedForCompletion,
                experienceFromQuestions,
                totalExperience
        );
    }
}