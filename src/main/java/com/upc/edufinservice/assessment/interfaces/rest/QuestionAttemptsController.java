package com.upc.edufinservice.assessment.interfaces.rest;

import com.upc.edufinservice.assessment.domain.services.AssessmentCommandService;
import com.upc.edufinservice.assessment.interfaces.rest.resources.QuestionAttemptResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitQuestionAttemptResource;
import com.upc.edufinservice.assessment.interfaces.rest.transform.QuestionAttemptResourceFromAggregateAssembler;
import com.upc.edufinservice.assessment.interfaces.rest.transform.SubmitQuestionAttemptCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

        // 1. EL BLINDAJE JWT: Extraemos el ID del alumno
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID safeUserId = UUID.fromString(username);

        // 2. Traducimos el JSON entrante a un Comando pasándole el ID seguro
        var command = SubmitQuestionAttemptCommandFromResourceAssembler.toCommandFromResource(resource, safeUserId);

        // 3. Ejecutamos la lógica de negocio
        var attempt = assessmentCommandService.handle(command);

        if (attempt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 4. Traducimos la respuesta a un JSON de salida
        var attemptResource = QuestionAttemptResourceFromAggregateAssembler.toResourceFromAggregate(attempt.get());
        return new ResponseEntity<>(attemptResource, HttpStatus.CREATED);
    }
}