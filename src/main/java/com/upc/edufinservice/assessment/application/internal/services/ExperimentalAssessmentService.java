package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.analytics.domain.model.entities.StudentInteraction;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.FastAPIClient;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.SolicitudPrediccionDto;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentPhase;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentResponse;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentSession;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalAssessmentResponseRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalAssessmentSessionRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalQuestionRepository;
import com.upc.edufinservice.assessment.interfaces.rest.resources.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ExperimentalAssessmentService {
    private static final int EXPECTED_QUESTION_COUNT = 12;
    private static final int MIN_DKT_INTERACTIONS = 8;

    private final ExperimentalQuestionRepository questionRepository;
    private final ExperimentalAssessmentSessionRepository sessionRepository;
    private final ExperimentalAssessmentResponseRepository responseRepository;
    private final StudentInteractionRepository interactionRepository;
    private final FastAPIClient fastApiClient;
    private final MasteryService masteryService;

    public ExperimentalAssessmentService(
            ExperimentalQuestionRepository questionRepository,
            ExperimentalAssessmentSessionRepository sessionRepository,
            ExperimentalAssessmentResponseRepository responseRepository,
            StudentInteractionRepository interactionRepository,
            FastAPIClient fastApiClient,
            MasteryService masteryService
    ) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.responseRepository = responseRepository;
        this.interactionRepository = interactionRepository;
        this.fastApiClient = fastApiClient;
        this.masteryService = masteryService;
    }

    @Transactional(readOnly = true)
    public List<ExperimentalQuestionResource> getQuestions() {
        return questionRepository.findAllByOrderByQuestionOrderAsc().stream()
                .map(q -> new ExperimentalQuestionResource(
                        q.getId(), q.getCode(), q.getQuestionOrder(), q.getModuleNumber(), q.getModuleName(),
                        q.getCompetency(), q.getQuestionText(),
                        q.getOptions().stream()
                                .map(o -> new ExperimentalOptionResource(o.getId(), o.getOptionText()))
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public ExperimentalSubmissionResponse submit(UUID userId, SubmitExperimentalAssessmentResource request) {
        if (userId == null) throw new IllegalArgumentException("userId es obligatorio.");
        if (request == null || request.phase() == null) throw new IllegalArgumentException("phase es obligatorio.");
        if (request.answers() == null) throw new IllegalArgumentException("answers es obligatorio.");

        ExperimentalAssessmentPhase phase = request.phase();
        if (sessionRepository.existsByUserIdAndPhase(userId, phase)) {
            throw new IllegalStateException("El usuario ya completó la fase " + phase + ".");
        }

        var questions = questionRepository.findAllByOrderByQuestionOrderAsc();
        if (questions.size() != EXPECTED_QUESTION_COUNT) {
            throw new IllegalStateException("El instrumento debe contener exactamente 12 preguntas.");
        }
        if (request.answers().size() != EXPECTED_QUESTION_COUNT) {
            throw new IllegalArgumentException("Se deben responder las 12 preguntas del instrumento.");
        }

        var answerByQuestion = request.answers().stream().collect(java.util.stream.Collectors.toMap(
                ExperimentalAnswerResource::questionId,
                a -> a,
                (a, b) -> { throw new IllegalArgumentException("No se permiten respuestas duplicadas para una pregunta."); }
        ));

        var expectedIds = questions.stream().map(q -> q.getId()).collect(java.util.stream.Collectors.toSet());
        if (!new HashSet<>(answerByQuestion.keySet()).equals(expectedIds)) {
            throw new IllegalArgumentException("Las respuestas no corresponden exactamente al banco experimental vigente.");
        }

        int correct = 0;
        java.util.ArrayList<Evaluated> evaluated = new java.util.ArrayList<>();

        for (var question : questions) {
            var answer = answerByQuestion.get(question.getId());
            var selectedOption = question.getOptions().stream()
                    .filter(o -> o.getId().equals(answer.selectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La opción seleccionada no pertenece a la pregunta " + question.getCode() + "."));
            if (Boolean.TRUE.equals(selectedOption.getIsCorrect())) correct++;
            evaluated.add(new Evaluated(question, selectedOption, answer.timeTakenSec()));
        }

        float score = ((float) correct / EXPECTED_QUESTION_COUNT) * 100.0f;
        var session = sessionRepository.save(new ExperimentalAssessmentSession(
                userId, phase, EXPECTED_QUESTION_COUNT, correct, score));

        for (var item : evaluated) {
            responseRepository.save(new ExperimentalAssessmentResponse(
                    session, item.question(), item.option(), item.option().getIsCorrect(), item.timeTakenSec()));
        }

        /*
         * PRE_TEST: además de conservarse como instrumento experimental, inicializa
         * el historial DKT. POST_TEST permanece fuera del modelo porque ocurre al
         * finalizar la intervención y no tiene utilidad adaptativa posterior.
         */
        if (phase == ExperimentalAssessmentPhase.PRE_TEST) {
            registerPreTestInDkt(userId, evaluated);
        }

        // El instrumento experimental sigue sin otorgar XP ni progreso de lecciones.
        return new ExperimentalSubmissionResponse(session.getId(), phase, EXPECTED_QUESTION_COUNT, session.getSubmittedAt());
    }

    private void registerPreTestInDkt(UUID userId, List<Evaluated> evaluated) {
        Instant baseTimestamp = Instant.now();

        for (var item : evaluated) {
            Integer skillId = item.question().getDktSkillId();

            if (skillId == null || skillId < 1 || skillId > 30) {
                throw new IllegalStateException(
                        "Pregunta experimental " + item.question().getCode()
                                + " sin dktSkillId válido (1..30)."
                );
            }

            var interaction = new StudentInteraction(
                    userId,
                    skillId,
                    Boolean.TRUE.equals(item.option().getIsCorrect()) ? 1 : 0,
                    InteractionType.PRE_TEST,
                    SelectionReason.STANDARD
            );

            // Garantiza el orden Q01..Q12 aun cuando se inserten en el mismo milisegundo.
            interaction.setInteractedAt(
                    baseTimestamp.plusMillis(item.question().getQuestionOrder())
            );

            interactionRepository.save(interaction);
        }

        updateDktSnapshot(userId);
    }

    private void updateDktSnapshot(UUID userId) {
        var history = interactionRepository.findByUserIdOrderByInteractedAtAsc(userId);

        if (history.size() < MIN_DKT_INTERACTIONS) {
            return;
        }

        var interactions = history.stream()
                .map(interaction -> new SolicitudPrediccionDto.InteraccionDto(
                        interaction.getDktSkillId(),
                        interaction.getIsCorrect() == 1,
                        interaction.getInteractedAt()
                ))
                .toList();

        var response = fastApiClient.obtenerPrediccion(
                new SolicitudPrediccionDto(userId.toString(), interactions)
        );

        if (response == null
                || !Boolean.TRUE.equals(response.modelReady())
                || response.mastery() == null
                || response.mastery().isEmpty()) {
            System.out.println(
                    "[DKT-FORGET] PRE_TEST registrado, pero el modelo no está disponible."
            );
            return;
        }

        masteryService.updateMasterySnapshot(userId, response.mastery());

        System.out.println(
                "[DKT-FORGET] Snapshot inicial actualizado desde PRE_TEST para usuario "
                        + userId
        );
    }

    private record Evaluated(
            com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalQuestion question,
            com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalOption option,
            Float timeTakenSec
    ) {}
}
