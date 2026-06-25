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
    private final UserLessonProgressRepository userLessonProgressRepository;

    public LearningQueryServiceImpl(TopicRepository topicRepository, LessonRepository lessonRepository, QuestionRepository questionRepository, QuestionOptionRepository questionOptionRepository
    , UserLessonProgressRepository userLessonProgressRepository) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.userLessonProgressRepository = userLessonProgressRepository;
    }

    @Override
    public List<Topic> handle(GetAllTopicsQuery query) {
        return topicRepository.findAll();
    }

    @Override
    public List<Lesson> handle(GetLessonsByTopicIdQuery query) {
        return lessonRepository.findByTopic_Id(query.topicId());
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

    @Override
    public TopicProgressMetrics handle(GetTopicProgressQuery query) {
        Integer total = lessonRepository.countByTopicId(query.topicId());
        Integer completed = userLessonProgressRepository.countCompletedLessons(query.userId(), query.topicId());

        // Protegemos contra nulos por si las tablas están vacías
        return new TopicProgressMetrics(
                total != null ? total : 0,
                completed != null ? completed : 0
        );
    }

    @Override
    public List<Question> handle(GetRandomQuestionsQuery query) {
        return questionRepository.findRandomQuestions(query.limit());
    }
}