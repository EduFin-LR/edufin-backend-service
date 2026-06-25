package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.UUID;

public record TopicResource(UUID id, String name, String category, Integer dktSkillId) {}
