package com.upc.edufinservice.assessment.interfaces.rest;

import com.upc.edufinservice.assessment.application.internal.services.DynamicFinalService;
import com.upc.edufinservice.assessment.application.internal.services.FinalCompletionService;
import com.upc.edufinservice.assessment.interfaces.rest.resources.DynamicFinalQuestionResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.DynamicFinalResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.CompleteFinalResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.FinalCompletionResponse;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.learning.interfaces.rest.transform.QuestionResourceFromAggregateAssembler;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/assessments/finals")
@Tag(
        name = "Dynamic Finals",
        description = "Construcción dinámica del examen final por módulo"
)
public class DynamicFinalController {

    private final DynamicFinalService dynamicFinalService;
    private final LearningQueryService learningQueryService;
    private final UserQueryService userQueryService;
    private final FinalCompletionService finalCompletionService;

    public DynamicFinalController(
            DynamicFinalService dynamicFinalService,
            LearningQueryService learningQueryService,
            UserQueryService userQueryService,
            FinalCompletionService finalCompletionService
    ) {
        this.dynamicFinalService = dynamicFinalService;
        this.learningQueryService = learningQueryService;
        this.userQueryService = userQueryService;
        this.finalCompletionService = finalCompletionService;
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<DynamicFinalResource> getDynamicFinal(
            @PathVariable UUID topicId
    ) {
        UUID userId = getSafeUserIdFromToken();

        var result = dynamicFinalService.buildFinal(
                userId,
                topicId
        );

        var resources = result.questions()
                .stream()
                .map(selection -> {
                    var options = learningQueryService.handle(
                            new GetOptionsByQuestionIdQuery(
                                    selection.question().getId()
                            )
                    );

                    var questionResource =
                            QuestionResourceFromAggregateAssembler
                                    .toResourceFromAggregate(
                                            selection.question(),
                                            options
                                    );

                    return new DynamicFinalQuestionResource(
                            questionResource,
                            selection.interactionType(),
                            selection.selectionReason()
                    );
                })
                .toList();

        return ResponseEntity.ok(
                new DynamicFinalResource(
                        result.topicId(),
                        result.adaptive(),
                        result.standardQuestionCount(),
                        result.adaptiveQuestionCount(),
                        resources
                )
        );
    }

    @PostMapping("/topics/{topicId}/complete")
    public ResponseEntity<FinalCompletionResponse> completeDynamicFinal(
            @PathVariable UUID topicId,
            @RequestBody CompleteFinalResource resource
    ) {
        UUID userId = getSafeUserIdFromToken();

        var response =
                finalCompletionService.completeFinal(
                        userId,
                        topicId,
                        resource.questionIds(),
                        resource.timeSpentSec()
                );

        return ResponseEntity.ok(response);
    }

    private UUID getSafeUserIdFromToken() {
        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        var userOpt = userQueryService.handle(
                new GetUserByUsernameQuery(
                        authentication.getName()
                )
        );

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no válido."
            );
        }

        return userOpt.get().getId();
    }
}
