package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.gamification.application.internal.services.XpRewardService;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionByIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class QuestionAnsweredCorrectlyEventHandler {
    private final XpRewardService xpRewardService;
    private final LearningQueryService learningQueryService;
    private final UserLessonProgressRepository lessonProgressRepository;
    private final QuestionAttemptRepository attemptRepository;

    public QuestionAnsweredCorrectlyEventHandler(
            XpRewardService xpRewardService,
            LearningQueryService learningQueryService,
            UserLessonProgressRepository lessonProgressRepository,
            QuestionAttemptRepository attemptRepository
    ) {
        this.xpRewardService = xpRewardService;
        this.learningQueryService = learningQueryService;
        this.lessonProgressRepository = lessonProgressRepository;
        this.attemptRepository = attemptRepository;
    }

    @EventListener
    public void on(QuestionAnsweredCorrectlyEvent event) {
        var question = learningQueryService.handle(new GetQuestionByIdQuery(event.questionId()))
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada al asignar XP"));
        UUID lessonId = question.getLesson().getId();

        // Si terminó la lección previamente, el repaso no entrega XP.
        // Esto protege también las lecciones anteriores a la migración del ledger.
        boolean completed = lessonProgressRepository
                .findByUserIdAndLessonId(event.userId(), lessonId)
                .map(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .orElse(false);
        if (completed) {
            return;
        }

        // Antes de esta corrección ya existían aciertos en question_attempts.
        // Respetamos esas recompensas y no volvemos a pagarlas tras el despliegue.
        boolean isDragDrop = "DRAG_AND_DROP".equalsIgnoreCase(question.getQuestionType());
        long correctAttemptsForSource = attemptRepository
                .findByUserIdAndQuestionId(event.userId(), event.questionId())
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .filter(a -> !isDragDrop || Objects.equals(a.getSelectedOptionId(), event.selectedOptionId()))
                .count();
        // El intento actual ya está guardado cuando se publica el evento.
        if (correctAttemptsForSource > 1) {
            return;
        }

        // En drag & drop premiamos una vez por tarjeta; en el resto, una vez
        // por pregunta, preservando la regla de 10 XP por respuesta atómica.
        UUID sourceId = isDragDrop && event.selectedOptionId() != null
                ? event.selectedOptionId() : event.questionId();
        xpRewardService.awardOnce(
                event.userId(), sourceId, XpRewardService.QUESTION_CORRECT, 10
        );
    }
}
