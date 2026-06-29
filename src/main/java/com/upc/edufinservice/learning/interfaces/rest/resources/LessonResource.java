package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.UUID;

public record LessonResource(
        UUID id,
        String title,
        String content,
        String videoUrl,
        Integer lessonOrder, //NUEVO: Para que React sepa el orden oficial de la lección
        String status // "LOCKED", "UNLOCKED", "IN_PROGRESS", "COMPLETED"
) {}
