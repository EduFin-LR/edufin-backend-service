package com.upc.edufinservice.assessment.interfaces.rest;

import com.upc.edufinservice.assessment.domain.services.AssessmentCommandService;
import com.upc.edufinservice.assessment.interfaces.rest.resources.QuestionAttemptResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitQuestionAttemptResource;
import com.upc.edufinservice.assessment.interfaces.rest.transform.QuestionAttemptResourceFromAggregateAssembler;
import com.upc.edufinservice.assessment.interfaces.rest.transform.SubmitQuestionAttemptCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attempts")
@Tag(name = "Assessment", description = "Endpoints para el registro de evaluaciones y respuestas")
public class QuestionAttemptsController {

    private final AssessmentCommandService assessmentCommandService;

    public QuestionAttemptsController(AssessmentCommandService assessmentCommandService) {
        this.assessmentCommandService = assessmentCommandService;
    }

    @PostMapping
    public ResponseEntity<QuestionAttemptResource> submitAttempt(@RequestBody SubmitQuestionAttemptResource resource) {
        // 1. Traducimos el JSON entrante a un Comando
        var command = SubmitQuestionAttemptCommandFromResourceAssembler.toCommandFromResource(resource);

        // 2. Ejecutamos la lógica de negocio (Guardar en BD y emitir Evento si es correcto)
        var attempt = assessmentCommandService.handle(command);

        if (attempt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 3. Traducimos la respuesta a un JSON de salida
        var attemptResource = QuestionAttemptResourceFromAggregateAssembler.toResourceFromAggregate(attempt.get());
        return new ResponseEntity<>(attemptResource, HttpStatus.CREATED);
    }
}