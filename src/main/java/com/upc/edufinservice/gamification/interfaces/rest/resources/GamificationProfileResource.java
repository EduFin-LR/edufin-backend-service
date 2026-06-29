package com.upc.edufinservice.gamification.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record GamificationProfileResource(
        UUID id,
        UUID userId,
        Integer totalPoints,
        Integer currentLevel,
        Integer streakDays,
        @JsonProperty("diagnostic_test")Boolean diagnosticTest
) {
}
