package com.upc.edufinservice.dashboard.interfaces.rest.resources;

public record GamificationSummaryResource(
        Integer streakDays,
        Integer currentLevel,
        Integer currentXp,
        Integer nextLevelXp
) {}
