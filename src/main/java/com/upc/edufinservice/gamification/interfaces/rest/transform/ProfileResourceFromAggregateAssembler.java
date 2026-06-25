package com.upc.edufinservice.gamification.interfaces.rest.transform;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.interfaces.rest.resources.GamificationProfileResource;

public class ProfileResourceFromAggregateAssembler {
    public static GamificationProfileResource toResourceFromAggregate(GamificationProfile profile) {
        return new GamificationProfileResource(
                profile.getId(),
                profile.getUserId(),
                profile.getTotalPoints(),
                profile.getCurrentLevel(),
                profile.getStreakDays()
        );
    }
}
