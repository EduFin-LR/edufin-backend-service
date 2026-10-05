package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentPhase;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentResponse;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentSession;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalAssessmentResponseRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalAssessmentSessionRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalQuestionRepository;
import com.upc.edufinservice.assessment.interfaces.rest.resources.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ExperimentalAssessmentService {
    private static final int EXPECTED_QUESTION_COUNT = 12;

    private final ExperimentalQuestionRepository questionRepository;
    private final ExperimentalAssessmentSessionRepository sessionRepository;
    private final ExperimentalAssessmentResponseRepository responseRepository;

    public ExperimentalAssessmentService(ExperimentalQuestionRepository questionRepository,
                                         ExperimentalAssessmentSessionRepository sessionRepository,
                                         ExperimentalAssessmentResponseRepository responseRepository) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.responseRepository = responseRepository;
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
        record Evaluated(com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalQuestion question,
                         com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalOption option,
                         Float timeTakenSec) {}
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

        // Intencionalmente NO publica eventos, NO da XP, NO crea StudentInteraction y NO llama al DKT.
        return new ExperimentalSubmissionResponse(session.getId(), phase, EXPECTED_QUESTION_COUNT, session.getSubmittedAt());
    }
}
