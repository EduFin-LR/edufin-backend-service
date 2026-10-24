package com.upc.edufinservice.gamification.domain.services;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;

import java.util.Optional;
import java.util.UUID;

public interface GamificationCommandService {
    Optional<GamificationProfile> handle(AddPointsCommand command);
    void evaluateModuleCompleted(UUID userId, int moduleNumber);
}
