package com.upc.edufinservice.assessment.interfaces.rest;

import java.util.UUID;

import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

        // 1. EL BLINDAJE JWT: Extraemos el username (email) del token
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        String currentUsername = authentication.getName();

        // 1.1 Buscamos al usuario en la BD usando ese username (Asegúrate de inyectar userQueryService en el constructor)
        var userOpt = userQueryService.handle(new GetUserByUsernameQuery(currentUsername));

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario asociado a este token no existe.");
        }

        // 1.2 Ahora sí tenemos el UUID seguro y real
        UUID safeUserId = userOpt.get().getId();

        // 2. Traducimos el JSON entrante a un Comando pasándole el ID seguro
        var command = SubmitQuestionAttemptCommandFromResourceAssembler.toCommandFromResource(resource, safeUserId);

        // 3. Ejecutamos la lógica de negocio
        var attempt = assessmentCommandService.handle(command);

        if (attempt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo registrar el intento.");
        }

        // 4. Traducimos la respuesta a un JSON de salida
        var attemptResource = QuestionAttemptResourceFromAggregateAssembler.toResourceFromAggregate(attempt.get());
        return new ResponseEntity<>(attemptResource, HttpStatus.CREATED);
    }
}