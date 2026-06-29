package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.UUID;

public record LessonResource(
        UUID id,
        String title,
        String content,
        String videoUrl,
        Integer lessonOrder,
        String lessonType, // ¡NUEVO!: Entrega si es "LESSON", "QUIZZ", "VIDEO" o "FINAL"
        String status // "LOCKED", "UNLOCKED", "IN_PROGRESS", "COMPLETED"
) {}
