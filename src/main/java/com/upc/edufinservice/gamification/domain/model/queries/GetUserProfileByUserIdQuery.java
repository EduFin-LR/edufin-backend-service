package com.upc.edufinservice.gamification.domain.model.queries;

import java.util.UUID;

public record GetUserProfileByUserIdQuery(UUID userId) {
    public GetUserProfileByUserIdQuery {
        if (userId == null) throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
    }
}