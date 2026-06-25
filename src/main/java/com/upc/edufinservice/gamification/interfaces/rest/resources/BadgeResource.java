package com.upc.edufinservice.gamification.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

public record BadgeResource(UUID id,
                            String name,
                            String description,
                            String iconUrl,
                            LocalDateTime earnedAt) {
}
