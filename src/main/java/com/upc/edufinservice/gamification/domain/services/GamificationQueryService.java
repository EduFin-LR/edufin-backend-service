package com.upc.edufinservice.gamification.domain.services;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetLeaderboardQuery;

import java.util.List;
import java.util.Optional;

public interface GamificationQueryService {
    Optional<GamificationProfile> handle(GetGamificationProfileByUserIdQuery query);
    List<GamificationProfile> handle(GetLeaderboardQuery query);
}


