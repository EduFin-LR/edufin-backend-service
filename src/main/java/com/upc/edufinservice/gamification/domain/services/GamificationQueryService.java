package com.upc.edufinservice.gamification.domain.services;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;

import java.util.Optional;

public interface GamificationQueryService {
    Optional<GamificationProfile> handle(GetGamificationProfileByUserIdQuery query);
}
