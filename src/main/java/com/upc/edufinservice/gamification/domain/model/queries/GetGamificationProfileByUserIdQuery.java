package com.upc.edufinservice.gamification.domain.model.queries;

import java.util.UUID;

public record GetGamificationProfileByUserIdQuery(UUID userId) {
    public GetGamificationProfileByUserIdQuery {
        if (userId == null) throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
    }
}
