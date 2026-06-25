package com.upc.edufinservice.gamification.domain.services;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;

import java.util.Optional;

public interface GamificationCommandService {
    Optional<GamificationProfile> handle(AddPointsCommand command);
}
