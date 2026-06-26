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

import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionsByLessonIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.learning.interfaces.rest.resources.QuestionResource;
import com.upc.edufinservice.learning.interfaces.rest.transform.QuestionResourceFromAggregateAssembler;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/lessons")
@Tag(name = "Learning - Lessons", description = "Cuestionarios de las lecciones")
public class LessonsController {

    private final LearningQueryService queryService;

    public LessonsController(LearningQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{lessonId}/questions")
    public ResponseEntity<List<QuestionResource>> getQuestionsByLessonId(@PathVariable UUID lessonId) {
        var questions = queryService.handle(new GetQuestionsByLessonIdQuery(lessonId));

        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron preguntas para la lección solicitada.");
        }

        var resources = questions.stream().map(question -> {
            var options = queryService.handle(new GetOptionsByQuestionIdQuery(question.getId()));

            if (options.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La pregunta " + question.getId() + " no tiene opciones configuradas.");
            }

            return QuestionResourceFromAggregateAssembler.toResourceFromAggregate(question, options);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }
}