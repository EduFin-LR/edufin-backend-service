package com.upc.edufinservice.dashboard.interfaces.rest.resources;

import java.util.List;

public record HomeDashboardResource(
        UserGreetingResource user,
        GamificationSummaryResource gamification,
        List<TopicProgressResource> learningPath
) {}
