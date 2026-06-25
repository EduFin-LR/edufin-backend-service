package com.upc.edufinservice.assessment.interfaces.rest;

import com.upc.edufinservice.assessment.interfaces.rest.resources.DiagnosticOptionResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.DiagnosticQuestionResource;
import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetRandomQuestionsQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assessments/diagnostic")
@Tag(name = "Diagnostic Assessment", description = "Endpoints para el examen inicial de calibración del modelo DKT")
public class DiagnosticAssessmentController {

    private final LearningQueryService learningQueryService;

    public DiagnosticAssessmentController(LearningQueryService learningQueryService) {
        this.learningQueryService = learningQueryService;
    }

    @GetMapping("/questions")
    public ResponseEntity<List<DiagnosticQuestionResource>> getDiagnosticQuestions(
            @RequestParam(defaultValue = "5") int limit // Por defecto trae 5, pero React puede pedir más
    ) {
        // 1. Pedir N preguntas al azar al módulo de Learning
        var preguntasRandom = learningQueryService.handle(new GetRandomQuestionsQuery(limit));

        List<DiagnosticQuestionResource> diagnosticTest = new ArrayList<>();

        // 2. Por cada pregunta, buscamos sus opciones y armamos el JSON
        for (var pregunta : preguntasRandom) {

            var opcionesReal = learningQueryService.handle(new GetOptionsByQuestionIdQuery(pregunta.getId()));

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
}