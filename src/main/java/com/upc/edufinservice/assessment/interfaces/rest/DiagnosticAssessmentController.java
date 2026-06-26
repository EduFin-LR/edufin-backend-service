package com.upc.edufinservice.assessment.interfaces.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.upc.edufinservice.assessment.application.internal.commandservices.DiagnosticCommandServiceImpl;
import com.upc.edufinservice.assessment.domain.model.commands.DiagnosticAnswerCommand;
import com.upc.edufinservice.assessment.domain.model.commands.EvaluateDiagnosticCommand;
import com.upc.edufinservice.assessment.interfaces.rest.resources.DiagnosticOptionResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.DiagnosticQuestionResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.DiagnosticResultResponseResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitDiagnosticResource;
import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetRandomQuestionsQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;

import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/assessments/diagnostic")
@Tag(name = "Diagnostic Assessment", description = "Endpoints para el examen inicial de calibración del modelo DKT")
public class DiagnosticAssessmentController {

    private final LearningQueryService learningQueryService;
    private final DiagnosticCommandServiceImpl _diagnosticCommandService;
    public DiagnosticAssessmentController(LearningQueryService learningQueryService,
                                          DiagnosticCommandServiceImpl diagnosticCommandService) {
        this.learningQueryService = learningQueryService;
        _diagnosticCommandService = diagnosticCommandService;
    }

    @GetMapping("/questions")
    public ResponseEntity<List<DiagnosticQuestionResource>> getDiagnosticQuestions(
            @RequestParam(defaultValue = "5") int limit // Por defecto trae 5, pero React puede pedir más
    ) {
                if (limit < 1) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El límite de preguntas debe ser mayor a cero.");
                }

        // 1. Pedir N preguntas al azar al módulo de Learning
        var preguntasRandom = learningQueryService.handle(new GetRandomQuestionsQuery(limit));

                if (preguntasRandom.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay preguntas disponibles para el diagnóstico.");
                }

        List<DiagnosticQuestionResource> diagnosticTest = new ArrayList<>();

        // 2. Por cada pregunta, buscamos sus opciones y armamos el JSON
        for (var pregunta : preguntasRandom) {

            var opcionesReal = learningQueryService.handle(new GetOptionsByQuestionIdQuery(pregunta.getId()));

                        if (opcionesReal.isEmpty()) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La pregunta " + pregunta.getId() + " no tiene opciones configuradas.");
                        }

            List<DiagnosticOptionResource> opcionesJson = opcionesReal.stream()
                    .map(opc -> new DiagnosticOptionResource(opc.getId(), opc.getOptionText()))
                    .toList();

            diagnosticTest.add(new DiagnosticQuestionResource(
                    pregunta.getId(),
                    pregunta.getQuestionText(),
                    opcionesJson
            ));
        }

        return ResponseEntity.ok(diagnosticTest);
    }

    @PostMapping("/submit")
    public ResponseEntity<DiagnosticResultResponseResource> submitDiagnostic(
            @RequestBody SubmitDiagnosticResource request
    ) {
        // 1. EL BLINDAJE JWT: Extraemos el ID del usuario del token
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
                        throw new MissingJwtException();
                }

                UUID safeUserId;
                try {
                        safeUserId = UUID.fromString(authentication.getName());
                } catch (IllegalArgumentException ex) {
                        throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token inválido o usuario no autenticado.");
                }

        // 2. Mapeamos el JSON (Resource) al Comando (Capa de Dominio)
        var answerCommands = request.answers().stream()
                .map(a -> new DiagnosticAnswerCommand(a.questionId(), a.selectedOptionId(), a.timeTakenSec()))
                .toList();

        var command = new EvaluateDiagnosticCommand(safeUserId, answerCommands);

        // 3. Ejecutamos el servicio seguro
        float finalScore = _diagnosticCommandService.evaluateDiagnostic(command);

        return ResponseEntity.ok(new DiagnosticResultResponseResource(
                finalScore,
                "Diagnostico completado. El modelo DKT ha sido calibrado."
        ));
    }
}