package com.upc.edufinservice.learning.domain.services;

import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
import com.upc.edufinservice.learning.domain.model.queries.*;

import java.util.List;
import java.util.Optional;

public interface LearningQueryService {
    List<Topic> handle(GetAllTopicsQuery query);
    List<Lesson> handle(GetLessonsByTopicIdQuery query);
    List<Question> handle(GetQuestionsByLessonIdQuery query);
    List<QuestionOption> handle(GetOptionsByQuestionIdQuery query);

    // Agrega este método a la interfaz existente:
    Topic handle(GetTopicByQuestionIdQuery query);

    List<Question> handle(GetRandomQuestionsQuery query);

    Optional<Topic> handle(GetTopicByIdQuery query);
}