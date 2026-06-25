package com.upc.edufinservice.gamification.application.internal.commandservices;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.aggregates.UserAchievement;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.BadgeRepository;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.GamificationProfileRepository;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.UserAchievementRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GamificationCommandServiceImpl implements GamificationCommandService {

    private final GamificationProfileRepository profileRepository;
    private final BadgeRepository badgeRepository;
    private final UserAchievementRepository achievementRepository;

    public GamificationCommandServiceImpl(GamificationProfileRepository profileRepository,
                                          BadgeRepository badgeRepository,
                                          UserAchievementRepository achievementRepository) {
        this.profileRepository = profileRepository;
        this.badgeRepository = badgeRepository;
        this.achievementRepository = achievementRepository;
    }

    @Override
    public Optional<GamificationProfile> handle(AddPointsCommand command) {
        var profile = profileRepository.findByUserId(command.userId())
                .orElseGet(() -> new GamificationProfile(command.userId()));

        // 1. Sumamos los puntos (esto actualiza el nivel y la racha automáticamente por tu lógica de dominio)
        profile.addPoints(command.points());
        var savedProfile = profileRepository.save(profile);

        // 2. Evaluamos si ganó alguna medalla nueva
        evaluateAchievements(savedProfile);

        return Optional.of(savedProfile);
    }

    private void evaluateAchievements(GamificationProfile profile) {
        var allBadges = badgeRepository.findAll();

        for (var badge : allBadges) {
            // Si ya la tiene, la saltamos
            if (achievementRepository.existsByUserIdAndBadgeId(profile.getUserId(), badge.getId())) {
                continue;
            }

            boolean conditionMet = false;

            // Evaluamos según el tipo de condición que definiste en tu SQL
            switch (badge.getConditionType().toUpperCase()) {
                case "LEVEL":
                    conditionMet = profile.getCurrentLevel() >= badge.getConditionValue();
                    break;
                case "POINTS":
                    conditionMet = profile.getTotalPoints() >= badge.getConditionValue();
                    break;
                case "STREAK":
                    conditionMet = profile.getStreakDays() >= badge.getConditionValue();
                    break;
            }

            // Si cumplió la meta, le damos su recompensa
            if (conditionMet) {
                var newAchievement = new UserAchievement(profile.getUserId(), badge);
                achievementRepository.save(newAchievement);

                System.out.println("🎉 ¡Logro Desbloqueado para el usuario " + profile.getUserId() + ": " + badge.getName() + "!");
                // (Opcional: Aquí podrías disparar un evento para enviar una notificación Push al frontend)
            }
        }
    }
}