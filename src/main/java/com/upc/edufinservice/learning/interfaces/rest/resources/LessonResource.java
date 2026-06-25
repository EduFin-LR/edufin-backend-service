package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.UUID;

public record LessonResource(UUID id, String title, String content, String videoUrl) {}
