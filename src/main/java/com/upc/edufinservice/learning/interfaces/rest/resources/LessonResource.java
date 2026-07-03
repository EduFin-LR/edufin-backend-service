package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.UUID;

public record LessonResource(
        UUID id,
        String title,
        String content,
        String videoUrl,
        Integer lessonOrder,
        String lessonType,
        String status,
        Integer dktSkillId //NUEVO: Permite trackear pasivamente los videos y lecturas
) {}
