package com.upc.edufinservice.gamification.interfaces.rest.resources;

import java.util.UUID;

public record LeaderboardProfileResource(
        UUID userId,
        String displayName, // 🚀 ¡Aquí irá el nombre o username!
        Integer totalPoints,
        Integer currentLevel,
        Integer streakDays
) {
}
