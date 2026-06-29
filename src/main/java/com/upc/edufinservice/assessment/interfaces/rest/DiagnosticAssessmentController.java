package com.upc.edufinservice.assessment.interfaces.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.upc.edufinservice.assessment.domain.model.commands.DiagnosticResponse;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
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
    private final UserQueryService userQueryService;

    public DiagnosticAssessmentController(LearningQueryService learningQueryService,
                                          DiagnosticCommandServiceImpl diagnosticCommandService,
                                          UserQueryService userQueryService) {
        this.learningQueryService = learningQueryService;
        _diagnosticCommandService = diagnosticCommandService;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/questions")
    public ResponseEntity<List<DiagnosticQuestionResource>> getDiagnosticQuestions(
            @RequestParam(defaultValue = "10") int limit // ¡CAMBIADO A 10! Por defecto jalará las 5 del Tema 1 y 5 del Tema 2
    ) {
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El límite de preguntas debe ser mayor a cero.");
        }

        var preguntasRandom = learningQueryService.handle(new GetRandomQuestionsQuery(limit));

        if (preguntasRandom.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay preguntas disponibles para el diagnóstico.");
        }

        List<DiagnosticQuestionResource> diagnosticTest = new ArrayList<>();

        for (var pregunta : preguntasRandom) {
            var opcionesReal = learningQueryService.handle(new GetOptionsByQuestionIdQuery(pregunta.getId()));

            if (opcionesReal.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La pregunta " + pregunta.getId() + " no tiene opciones.");
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
    public ResponseEntity<DiagnosticResponse> submitDiagnostic(@RequestBody SubmitDiagnosticResource request) {
        // Blindaje JWT Seguro
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        String currentUsername = authentication.getName();
        var userOpt = userQueryService.handle(new GetUserByUsernameQuery(currentUsername));

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario no existe.");
        }

        UUID safeUserId = userOpt.get().getId();

        // Mapeamos incluyendo de forma segura el match_category opcional por si hay arrastres
        var answerCommands = request.answers().stream()
                .map(a -> new DiagnosticAnswerCommand(a.questionId(), a.selectedOptionId(), a.selectedMatchCategory(), a.timeTakenSec()))
                .toList();

        var command = new EvaluateDiagnosticCommand(safeUserId, answerCommands);

        // Ejecutamos el servicio que calcula el perfil lúdico completo
        DiagnosticResponse response = _diagnosticCommandService.evaluateDiagnostic(command);

        // Devolvemos directamente el objeto enriquecido a React
        return ResponseEntity.ok(response);
    }
}