package com.upc.edufinservice.gamification.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

public record BadgeProgressResource(UUID id,
                                    String name,
                                    String description,
                                    String iconUrl,
                                    boolean isUnlocked, // La clave para tu frontend
                                    // sera null si aun no lo desbloquea
                                    LocalDateTime earnedAt ) {
}
