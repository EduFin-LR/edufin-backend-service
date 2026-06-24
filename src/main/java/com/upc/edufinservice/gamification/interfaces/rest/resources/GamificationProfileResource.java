package com.upc.edufinservice.gamification.interfaces.rest.resources;

import java.util.UUID;

public record GamificationProfileResource(UUID id, UUID userId, Integer totalPoints, Integer currentLevel, Integer streakDays) {
}
