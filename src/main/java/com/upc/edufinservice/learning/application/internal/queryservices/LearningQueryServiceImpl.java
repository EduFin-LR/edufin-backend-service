package com.upc.edufinservice.learning.application.internal.queryservices;

import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
import com.upc.edufinservice.learning.domain.model.queries.*;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningQueryServiceImpl implements LearningQueryService {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    public LearningQueryServiceImpl(TopicRepository topicRepository, LessonRepository lessonRepository, QuestionRepository questionRepository, QuestionOptionRepository questionOptionRepository) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
    }

    @Override
    public List<Topic> handle(GetAllTopicsQuery query) {
        return topicRepository.findAll();
    }

    @Override
    public List<Lesson> handle(GetLessonsByTopicIdQuery query) {
        return lessonRepository.findByTopicId(query.topicId());
    }

    @Override
    public List<Question> handle(GetQuestionsByLessonIdQuery query) {
        return questionRepository.findByLessonId(query.lessonId());
    }

    @Override
    public List<QuestionOption> handle(GetOptionsByQuestionIdQuery query) {
        return questionOptionRepository.findByQuestionId(query.questionId());
    }

    // Agrega esta implementación al servicio existente:
    @Override
    public Topic handle(GetTopicByQuestionIdQuery query) {
        var question = questionRepository.findById(query.questionId())
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));

        // Gracias a la relación fuerte física, podemos navegar: Pregunta -> Lección -> Tema
        return question.getLesson().getTopic();
    }
}