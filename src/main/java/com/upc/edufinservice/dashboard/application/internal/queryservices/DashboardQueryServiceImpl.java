package com.upc.edufinservice.dashboard.application.internal.queryservices;

import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.MlPredictionRepository;
import com.upc.edufinservice.assessment.domain.services.AssessmentQueryService;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.GamificationSummaryResource;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.HomeDashboardResource;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.TopicProgressResource;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.UserGreetingResource;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.queries.GetAllTopicsQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetLessonsByTopicIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetTopicProgressQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardQueryServiceImpl {

    private final GamificationQueryService gamificationQueryService;
    private final LearningQueryService learningQueryService;
    private final AssessmentQueryService assessmentQueryService; // Inyectado
    private final MlPredictionRepository mlPredictionRepository;
    private final UserQueryService userQueryService;

    public DashboardQueryServiceImpl(GamificationQueryService gamificationQueryService,
                                     LearningQueryService learningQueryService,
                                     AssessmentQueryService assessmentQueryService, // Agregado
                                     MlPredictionRepository mlPredictionRepository,
                                     UserQueryService userQueryService) {
        this.gamificationQueryService = gamificationQueryService;
        this.learningQueryService = learningQueryService;
        this.assessmentQueryService = assessmentQueryService;
        this.mlPredictionRepository = mlPredictionRepository;
        this.userQueryService = userQueryService;
    }

    public HomeDashboardResource getHomeDashboard(UUID userId) {
        // 1. OBTENER DATOS REALES DE GAMIFICACIÓN
        var profileQuery = new GetGamificationProfileByUserIdQuery(userId);
        var gamificationProfile = gamificationQueryService.handle(profileQuery);

        GamificationSummaryResource gamificationSummary;
        if (gamificationProfile.isPresent()) {
            var profile = gamificationProfile.get();
            int currentXp = profile.getTotalPoints();
            int currentLevel = profile.getCurrentLevel();
            int nextLevelXp = currentLevel * 400;

            gamificationSummary = new GamificationSummaryResource(
                    profile.getStreakDays(), currentLevel, currentXp, nextLevelXp
            );
        } else {
            gamificationSummary = new GamificationSummaryResource(0, 1, 0, 400);
        }

        // 2. DATOS REALES DE IDENTIDAD (IAM)
        var userOpt = userQueryService.handle(new GetUserByIdQuery(userId));
        String nombreMostrar = "Estudiante EduFin";
        String avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId;

        if (userOpt.isPresent()) {
            var user = userOpt.get();
            if (user.getFullName() != null && !user.getFullName().isBlank()) {
                nombreMostrar = user.getFullName();
            } else if (user.getUsername() != null && !user.getUsername().isBlank()) {
                nombreMostrar = user.getUsername();
            }
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                avatarUrl = user.getAvatarUrl();
            }
        }
        var userGreeting = new UserGreetingResource(nombreMostrar, avatarUrl);

        // 3. OBTENER TEMAS REALES Y APLICAR RECOMENDACIÓN DE LA IA (DKT)
        var topics = learningQueryService.handle(new GetAllTopicsQuery());
        List<TopicProgressResource> learningPath = new ArrayList<>();

        UUID topicIdRecomendado = null;
        float menorProbabilidad = 1.1f;

        for (var topic : topics) {
            var prediccionOpt = mlPredictionRepository.findByUserIdAndTopicId(userId, topic.getId());
            if (prediccionOpt.isPresent()) {
                float probabilidadExito = prediccionOpt.get().getPredictedSuccessProbability();
                if (probabilidadExito < menorProbabilidad) {
                    menorProbabilidad = probabilidadExito;
                    topicIdRecomendado = topic.getId();
                }
            }
        }


        // 4. CONSTRUIR EL PROGRESO REAL
        for (int i = 0; i < topics.size(); i++) {
            var topic = topics.get(i);

            // A. Le pedimos a Learning las lecciones oficiales de este tema
            var lessons = learningQueryService.handle(new GetLessonsByTopicIdQuery(topic.getId()));
            int totalLecciones = lessons.size();

            // B. Extraemos los IDs de las lecciones
            List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();

            // C. Le preguntamos a Assessment cuántas de esas lecciones específicas completó el usuario
            int leccionesCompletadas = assessmentQueryService.getCompletedLessonsCount(userId, lessonIds);

            int porcentaje = totalLecciones > 0 ? (leccionesCompletadas * 100) / totalLecciones : 0;

            // Por defecto, asumimos que el tema macro está bloqueado
            String status = ProgressStatus.LOCKED.name();

            // Regla de oro: Está disponible si es la primera unidad o si la unidad previa ya se completó
            boolean isTopicAvailable = (i == 0) || ProgressStatus.COMPLETED.name().equals(learningPath.get(i - 1).status());

            if (isTopicAvailable) {
                if (totalLecciones > 0 && leccionesCompletadas == totalLecciones) {
                    status = ProgressStatus.COMPLETED.name();
                } else if (leccionesCompletadas > 0) {
                    status = ProgressStatus.IN_PROGRESS.name();
                } else {
                    // Si completed == 0: Evaluamos si ya inició formalmente la primera lección del tema
                    boolean hasStartedFirstLesson = false;
                    if (!lessons.isEmpty()) {
                        boolean isFirstLessonOfApp = (i == 0);
                        String firstLessonStatus = assessmentQueryService.getLessonStatus(userId, lessons.get(0).getId(), isFirstLessonOfApp);

                        if (ProgressStatus.IN_PROGRESS.name().equals(firstLessonStatus)) {
                            hasStartedFirstLesson = true;
                        }
                    }
                    // Si ya le dio a "start", pasa a IN_PROGRESS; sino, se queda listo para jugar (UNLOCKED)
                    status = hasStartedFirstLesson ? ProgressStatus.IN_PROGRESS.name() : ProgressStatus.UNLOCKED.name();
                }
            }

            boolean isAiRecommended = topic.getId().equals(topicIdRecomendado);

            learningPath.add(new TopicProgressResource(
                    topic.getId(), topic.getName(), leccionesCompletadas, totalLecciones, porcentaje, status, isAiRecommended
            ));
        }

        if (topicIdRecomendado == null && !learningPath.isEmpty()) {
            var primerTema = learningPath.get(0);
            learningPath.set(0, new TopicProgressResource(
                    primerTema.topicId(), primerTema.topicName(), primerTema.completedLessons(),
                    primerTema.totalLessons(), primerTema.progressPercentage(), primerTema.status(), true
            ));
        }

        return new HomeDashboardResource(userGreeting, gamificationSummary, learningPath);
    }
}