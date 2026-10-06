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
        Integer dktSkillId, // Permite trackear pasivamente los videos y lecturas
        Integer stars       // 0 sin intento; 1 = 0-40%; 2 = 50-80%; 3 = 90-100%
) {}
