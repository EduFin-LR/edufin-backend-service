package com.upc.edufinservice.dashboard.application.internal.queryservices;

import com.upc.edufinservice.dashboard.interfaces.rest.resources.GamificationSummaryResource;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.HomeDashboardResource;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.TopicProgressResource;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.UserGreetingResource;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DashboardQueryServiceImpl {

    private final GamificationQueryService gamificationQueryService;
    // TODO: Más adelante inyectaremos el LearningQueryService y el IAMQueryService

    public DashboardQueryServiceImpl(GamificationQueryService gamificationQueryService) {
        this.gamificationQueryService = gamificationQueryService;
    }

    public HomeDashboardResource getHomeDashboard(UUID userId) {
        // 1. OBTENER DATOS REALES DE GAMIFICACIÓN
        var profileQuery = new GetGamificationProfileByUserIdQuery(userId);
        var gamificationProfile = gamificationQueryService.handle(profileQuery);

        GamificationSummaryResource gamificationSummary;

        if (gamificationProfile.isPresent()) {
            var profile = gamificationProfile.get();
            // Lógica simple para calcular XP para el próximo nivel (Ej: Nivel * 400)
            int currentXp = profile.getTotalPoints();
            int currentLevel = profile.getCurrentLevel();
            int nextLevelXp = currentLevel * 400;

            gamificationSummary = new GamificationSummaryResource(
                    profile.getStreakDays(),
                    currentLevel,
                    currentXp,
                    nextLevelXp
            );
        } else {
            // Valores por defecto si el usuario es nuevo
            gamificationSummary = new GamificationSummaryResource(0, 1, 0, 400);
        }

        // 2. DATOS SIMULADOS DE USUARIO (IAM)
        // TODO: Reemplazar con llamada real a IAM
        var userGreeting = new UserGreetingResource(
                "Jose Carlos",
                "https://api.dicebear.com/7.x/avataaars/svg?seed=Jose" // Avatar dinámico temporal
        );

        // 3. DATOS SIMULADOS DE APRENDIZAJE E INTELIGENCIA ARTIFICIAL
        // TODO: Reemplazar con llamada real a LearningQueryService y AnalyticsQueryService
        var tema1 = new TopicProgressResource(
                UUID.randomUUID(),
                "Ahorro Básico",
                10,
                12,
                83,
                "IN_PROGRESS",
                false
        );

        var tema2 = new TopicProgressResource(
                UUID.randomUUID(),
                "Presupuesto Personal",
                0,
                15,
                0,
                "PENDING",
                true // Simulando que la IA detectó que necesita este tema
        );

        var learningPath = List.of(tema1, tema2);

        // 4. ENSAMBLAR Y RETORNAR EL JSON MAESTRO
        return new HomeDashboardResource(userGreeting, gamificationSummary, learningPath);
    }
}