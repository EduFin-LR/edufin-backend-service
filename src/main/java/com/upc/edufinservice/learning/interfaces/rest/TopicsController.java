package com.upc.edufinservice.learning.interfaces.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.upc.edufinservice.learning.domain.model.queries.GetAllTopicsQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetLessonsByTopicIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.learning.interfaces.rest.resources.LessonResource;
import com.upc.edufinservice.learning.interfaces.rest.resources.TopicResource;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/topics")
@Tag(name = "Learning - Topics", description = "Catálogo de temas y lecciones financieras")
public class TopicsController {

    private final LearningQueryService queryService;

    public TopicsController(LearningQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<List<TopicResource>> getAllTopics() {
        var topics = queryService.handle(new GetAllTopicsQuery());

        if (topics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay temas disponibles.");
        }

        var resources = topics.stream()
                .map(t -> new TopicResource(t.getId(), t.getName(), t.getCategory(), t.getDktSkillId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{topicId}/lessons")
    public ResponseEntity<List<LessonResource>> getLessonsByTopicId(@PathVariable UUID topicId) {
        var lessons = queryService.handle(new GetLessonsByTopicIdQuery(topicId));

        if (lessons.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay lecciones para el tema solicitado.");
        }

        var resources = lessons.stream()
                .map(l -> new LessonResource(l.getId(), l.getTitle(), l.getContent(), l.getVideoUrl()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }
}