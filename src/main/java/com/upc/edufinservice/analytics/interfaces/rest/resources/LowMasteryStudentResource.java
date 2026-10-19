package com.upc.edufinservice.analytics.interfaces.rest.resources;

import java.util.UUID;

public record LowMasteryStudentResource(
        UUID userId,
        String username,
        String email,
        double currentMasteryAverage,
        long lowMasterySkills
) {
}