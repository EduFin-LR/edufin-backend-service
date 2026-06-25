package com.upc.edufinservice.gamification.domain.model.commands;

import java.util.UUID;

public record AddPointsCommand(UUID userId, Integer points) {
    public AddPointsCommand {
        if (userId == null) throw new IllegalArgumentException("El ID del usuario es obligatorio");
        if (points == null || points <= 0) throw new IllegalArgumentException("Los puntos a sumar deben ser mayores a cero");
    }
}
