package com.upc.edufinservice.learning.interfaces.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.upc.edufinservice.assessment.domain.services.AssessmentQueryService;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.upc.edufinservice.learning.domain.model.queries.GetAllTopicsQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetLessonsByTopicIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.learning.interfaces.rest.resources.LessonResource;
import com.upc.edufinservice.learning.interfaces.rest.resources.TopicResource;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/topics")
@Tag(name = "Learning - Topics", description = "Catálogo de temas y lecciones financieras con tracking de progreso")
public class TopicsController {

    private final LearningQueryService queryService;
    private final AssessmentQueryService assessmentQueryService; // Inyectado
    private final UserQueryService userQueryService;             // Inyectado

    public TopicsController(LearningQueryService queryService,
                            AssessmentQueryService assessmentQueryService,
                            UserQueryService userQueryService) {
        this.queryService = queryService;
        this.assessmentQueryService = assessmentQueryService;
        this.userQueryService = userQueryService;
    }

    @GetMapping
    public ResponseEntity<List<TopicResource>> getAllTopics() {
        UUID safeUserId = getSafeUserIdFromToken();
        var topics = queryService.handle(new GetAllTopicsQuery());

        if (topics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay temas disponibles.");
        }

        List<TopicResource> enrichedTopics = new ArrayList<>();

        for (int i = 0; i < topics.size(); i++) {
            var t = topics.get(i);

            // 1. Buscamos las lecciones de este tema para calcular métricas
            var lessons = queryService.handle(new GetLessonsByTopicIdQuery(t.getId()));
            int totalLessons = lessons.size();

            List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();
            int completedLessons = assessmentQueryService.getCompletedLessonsCount(safeUserId, lessonIds);

            // 2. Determinamos el estado macro del Tópico (Alcancía Visual)
            // Por defecto, asumimos que toda unidad nace bloqueada (LOCKED)
            String topicStatus = ProgressStatus.LOCKED.name();

            // Regla de secuencialidad macro: Está disponible si es el primerísimo tema de la app,
            // o si el tema anterior en el ciclo ya se encuentra en estado COMPLETED
            boolean isTopicAvailable = (i == 0) || ProgressStatus.COMPLETED.name().equals(enrichedTopics.get(i - 1).status());

            if (isTopicAvailable) {
                if (totalLessons > 0 && completedLessons == totalLessons) {
                    topicStatus = ProgressStatus.COMPLETED.name();
                } else if (completedLessons > 0) {
                    topicStatus = ProgressStatus.IN_PROGRESS.name();
                } else {
                    // Si completedLessons == 0: Evaluamos de forma cruzada si ya inició la primera lección
                    boolean hasStartedFirstLesson = false;
                    if (!lessons.isEmpty()) {
                        boolean isFirstLessonOfApp = (i == 0);
                        String firstLessonStatus = assessmentQueryService.getLessonStatus(safeUserId, lessons.get(0).getId(), isFirstLessonOfApp);

                        if (ProgressStatus.IN_PROGRESS.name().equals(firstLessonStatus)) {
                            hasStartedFirstLesson = true;
                        }
                    }
                    // Si ya le dio click a "start", la alcancía se marca IN_PROGRESS; de lo contrario, queda UNLOCKED (abierta a jugar)
                    topicStatus = hasStartedFirstLesson ? ProgressStatus.IN_PROGRESS.name() : ProgressStatus.UNLOCKED.name();
                }
            }

            // Si es el segundo tema en adelante y el anterior no está completo, se renderiza bloqueado en el mapa
            if (i > 0 && !"COMPLETED".equals(enrichedTopics.get(i - 1).status())) {
                topicStatus = "LOCKED";
            }

            enrichedTopics.add(new TopicResource(
                    t.getId(), t.getName(), t.getCategory(), t.getDktSkillId(),
                    completedLessons, totalLessons, topicStatus
            ));
        }

        return ResponseEntity.ok(enrichedTopics);
    }

    @GetMapping("/{topicId}/lessons")
    public ResponseEntity<List<LessonResource>> getLessonsByTopicId(@PathVariable UUID topicId) {
        UUID safeUserId = getSafeUserIdFromToken();
        var lessons = queryService.handle(new GetLessonsByTopicIdQuery(topicId));

        if (lessons.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay lecciones para el tema solicitado.");
        }

        // Jalamos la lista general de todos los temas ordenados para saber cuál es el primerísimo nivel del juego
        var allTopics = queryService.handle(new GetAllTopicsQuery());
        UUID firstTopicId = allTopics.isEmpty() ? null : allTopics.get(0).getId();

        List<LessonResource> enrichedLessons = new ArrayList<>();

        for (var l : lessons) {
            // Regla de control: Es la primerísima lección si pertenece al Topic 1 y su lessonOrder es 1
            boolean isFirstLessonOfApp = topicId.equals(firstTopicId) && l.getLessonOrder() == 1;

            // Le consultamos al motor analítico el estado real de este nivel para el alumno
            String status = assessmentQueryService.getLessonStatus(safeUserId, l.getId(), isFirstLessonOfApp);

            enrichedLessons.add(new LessonResource(
                    l.getId(), l.getTitle(), l.getContent(), l.getVideoUrl(), status
            ));
        }

        return ResponseEntity.ok(enrichedLessons);
    }

    // Método privado y seguro para extraer la identidad del estudiante desde el JWT
    private UUID getSafeUserIdFromToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }
        var userOpt = userQueryService.handle(new GetUserByUsernameQuery(authentication.getName()));
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no válido de la sesión.");
        }
        return userOpt.get().getId();
    }
}