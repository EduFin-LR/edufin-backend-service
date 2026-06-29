package com.upc.edufinservice.assessment.interfaces.rest;

import java.util.UUID;

import com.upc.edufinservice.assessment.domain.model.commands.CompleteLessonCommand;
import com.upc.edufinservice.assessment.domain.model.commands.LessonCompletionResponse;
import com.upc.edufinservice.assessment.domain.model.commands.StartLessonCommand;
import com.upc.edufinservice.assessment.interfaces.rest.resources.CompleteLessonResource;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.upc.edufinservice.assessment.domain.services.AssessmentCommandService;
import com.upc.edufinservice.assessment.interfaces.rest.resources.QuestionAttemptResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitQuestionAttemptResource;
import com.upc.edufinservice.assessment.interfaces.rest.transform.QuestionAttemptResourceFromAggregateAssembler;
import com.upc.edufinservice.assessment.interfaces.rest.transform.SubmitQuestionAttemptCommandFromResourceAssembler;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/attempts")
@Tag(name = "Assessment", description = "Endpoints para el registro de evaluaciones y respuestas")
public class QuestionAttemptsController {

    private final AssessmentCommandService assessmentCommandService;
    private final UserQueryService userQueryService;
    public QuestionAttemptsController(AssessmentCommandService assessmentCommandService,
                                      UserQueryService userQueryService) {
        this.assessmentCommandService = assessmentCommandService;
        this.userQueryService = userQueryService;
    }

    @PostMapping
    public ResponseEntity<QuestionAttemptResource> submitAttempt(@RequestBody SubmitQuestionAttemptResource resource) {
        UUID safeUserId = getSafeUserIdFromToken();
        var command = SubmitQuestionAttemptCommandFromResourceAssembler.toCommandFromResource(resource, safeUserId);
        var attempt = assessmentCommandService.handle(command);

        if (attempt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo registrar el intento.");
        }

        var attemptResource = QuestionAttemptResourceFromAggregateAssembler.toResourceFromAggregate(attempt.get());
        return new ResponseEntity<>(attemptResource, HttpStatus.CREATED);
    }

    // ========================================================================
    // NUEVO ENDPOINT 1: Inicializar Lección (Cambia estado a IN_PROGRESS)
    // ========================================================================
    @PostMapping("/lessons/{lessonId}/start")
    public ResponseEntity<String> startLesson(@PathVariable UUID lessonId) {
        UUID safeUserId = getSafeUserIdFromToken();
        try {
            assessmentCommandService.handle(new StartLessonCommand(safeUserId, lessonId));
            return ResponseEntity.ok("Lección iniciada correctamente en el motor de tracking.");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    // ========================================================================
    // NUEVO ENDPOINT 2: Finalizar Lección (Calcula nota y desbloquea el siguiente nivel)
    // ========================================================================
    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<LessonCompletionResponse> completeLesson(
            @PathVariable UUID lessonId,
            @RequestBody CompleteLessonResource resource
    ) {
        UUID safeUserId = getSafeUserIdFromToken();

        // Ejecutamos el comando y capturamos las métricas calculadas internamente
        LessonCompletionResponse response = assessmentCommandService.handle(new CompleteLessonCommand(
                safeUserId,
                lessonId,
                resource.timeSpentSec()
        ));

        // Devolvemos el JSON con el conteo de buenas y malas a React
        return ResponseEntity.ok(response);
    }

    // Método privado reutilizable para no duplicar el blindaje JWT
    private UUID getSafeUserIdFromToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }
        var userOpt = userQueryService.handle(new GetUserByUsernameQuery(authentication.getName()));
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no válido.");
        }
        return userOpt.get().getId();
    }
}