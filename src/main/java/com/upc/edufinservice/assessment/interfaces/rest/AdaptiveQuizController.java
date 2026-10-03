package com.upc.edufinservice.assessment.interfaces.rest;

import com.upc.edufinservice.assessment.application.internal.services.AdaptiveQuizService;
import com.upc.edufinservice.assessment.interfaces.rest.resources.AdaptiveQuizQuestionResource;
import com.upc.edufinservice.assessment.interfaces.rest.resources.AdaptiveQuizResource;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/assessments/adaptive-quizzes")
@Tag(
        name = "Adaptive Quizzes",
        description = "Construcción de quizzes con refuerzo por bajo mastery"
)
public class AdaptiveQuizController {

    private final AdaptiveQuizService adaptiveQuizService;
    private final LearningQueryService learningQueryService;
    private final UserQueryService userQueryService;

    public AdaptiveQuizController(
            AdaptiveQuizService adaptiveQuizService,
            LearningQueryService learningQueryService,
            UserQueryService userQueryService
    ) {
        this.adaptiveQuizService = adaptiveQuizService;
        this.learningQueryService = learningQueryService;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<AdaptiveQuizResource> getAdaptiveQuiz(
            @PathVariable UUID lessonId
    ) {
        UUID userId = getSafeUserIdFromToken();

        var result = adaptiveQuizService.buildQuiz(
                userId,
                lessonId
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

                    return new AdaptiveQuizQuestionResource(
                            questionResource,
                            selection.interactionType(),
                            selection.selectionReason()
                    );
                })
                .toList();

        return ResponseEntity.ok(
                new AdaptiveQuizResource(
                        result.adaptive(),
                        result.standardQuestionCount(),
                        result.reinforcementQuestionCount(),
                        resources
                )
        );
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
