package com.upc.edufinservice.assessment.interfaces.rest;

import com.upc.edufinservice.assessment.application.internal.services.ExperimentalAssessmentService;
import com.upc.edufinservice.assessment.interfaces.rest.resources.ExperimentalQuestionResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.ExperimentalSubmissionResponse;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitExperimentalAssessmentResource;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assessments/experimental")
@Tag(name = "Experimental Assessment", description = "Pre-test/Post-test de investigación; aislado de DKT, XP y progreso")
public class ExperimentalAssessmentController {
    private final ExperimentalAssessmentService service;
    private final UserQueryService userQueryService;

    public ExperimentalAssessmentController(ExperimentalAssessmentService service, UserQueryService userQueryService) {
        this.service = service;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/questions")
    public ResponseEntity<List<ExperimentalQuestionResource>> getQuestions() {
        return ResponseEntity.ok(service.getQuestions());
    }

    @PostMapping("/submit")
    public ResponseEntity<ExperimentalSubmissionResponse> submit(@RequestBody SubmitExperimentalAssessmentResource request) {
        UUID userId = currentUserId();
        try {
            return ResponseEntity.ok(service.submit(userId, request));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    private UUID currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }
        var user = userQueryService.handle(new GetUserByUsernameQuery(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario no existe."));
        return user.getId();
    }
}
