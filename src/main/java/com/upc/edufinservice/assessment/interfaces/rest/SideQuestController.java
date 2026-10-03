package com.upc.edufinservice.assessment.interfaces.rest;

import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.MlPredictionRepository;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SideQuestResource;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.learning.domain.model.queries.GetSideQuestQuestionsBySkillQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionByIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.learning.interfaces.rest.resources.QuestionResource;
import com.upc.edufinservice.learning.interfaces.rest.resources.QuestionOptionResource;
import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assessments/side-quest")
@Tag(name = "Side Quests IA", description = "Endpoints para misiones secundarias de reforzamiento adaptativo")
public class SideQuestController {

    private final MlPredictionRepository mlPredictionRepository;
    private final LearningQueryService learningQueryService;
    private final UserQueryService userQueryService;

    public SideQuestController(MlPredictionRepository mlPredictionRepository,
                               LearningQueryService learningQueryService,
                               UserQueryService userQueryService) {
        this.mlPredictionRepository = mlPredictionRepository;
        this.learningQueryService = learningQueryService;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<SideQuestResource> getSideQuestByLesson(@PathVariable UUID lessonId) {
        UUID safeUserId = getSafeUserIdFromToken();

        // 1. Verificamos si la IA activó un flag de repaso para esta lección específica
        var predictionOpt = mlPredictionRepository.findByUserIdAndRecommendedLessonId(safeUserId, lessonId);

        if (predictionOpt.isEmpty()) {
            // No hay crisis cognitiva: informamos a React que continúe el flujo normal
            return ResponseEntity.ok(new SideQuestResource(false, "No hay misiones secundarias activas para esta estación.", new ArrayList<>()));
        }

        // 2. Si existe, significa que el usuario está en Nivel 1. Generamos las preguntas de la habilidad en crisis.
        // Como las preguntas de esa habilidad comparten el mismo dkt_skill_id, podemos usar cualquiera como pivote
        var preguntasDeLaLeccion = learningQueryService.handle(new com.upc.edufinservice.learning.domain.model.queries.GetQuestionsByLessonIdQuery(lessonId));

        if (preguntasDeLaLeccion.isEmpty()) {
            return ResponseEntity.ok(new SideQuestResource(false, "Lección sin banco de preguntas disponible.", new ArrayList<>()));
        }

        Integer skillIdEnCrisis = preguntasDeLaLeccion.get(0).getSkill().getId();

        // 3. Invocamos nuestro query aleatorio de reforzamiento (Jala 3 preguntas de tipo QUIZ al azar)
        var preguntasRefuerzo = learningQueryService.handle(new GetSideQuestQuestionsBySkillQuery(skillIdEnCrisis, 3));

        // 4. Enriquecemos las preguntas con sus respectivas opciones para que Jackson las serialice bien hacia React
        List<QuestionResource> questionResources = preguntasRefuerzo.stream().map(question -> {
            var opciones = learningQueryService.handle(new GetOptionsByQuestionIdQuery(question.getId()));

            List<QuestionOptionResource> optionResources = opciones.stream()
                    .map(op -> new QuestionOptionResource(op.getId(), op.getOptionText(), op.getIsCorrect(), op.getMatchCategory()))
                    .toList();

            return new QuestionResource(
                    question.getId(),
                    question.getQuestionText(),
                    question.getExplanation(),
                    question.getQuestionType(),
                    question.getHint(),
                    question.getSuccessMessage(),
                    question.getErrorMessage(),
                    question.getSkill().getId(),
                    question.getTheoryText(),
                    optionResources
            );
        }).toList();

        return ResponseEntity.ok(new SideQuestResource(
                true,
                "¡Misión Secundaria Detectada! Tu retención en este concepto ha disminuido. Supera este reto de reforzamiento para recuperar tu ritmo.",
                questionResources
        ));
    }

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