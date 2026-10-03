package com.upc.edufinservice.learning.application.internal.queryservices;

import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
import com.upc.edufinservice.learning.domain.model.queries.*;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LearningQueryServiceImpl implements LearningQueryService {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    public LearningQueryServiceImpl(
            TopicRepository topicRepository,
            LessonRepository lessonRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository questionOptionRepository) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
    }

    @Override
    public List<Topic> handle(GetAllTopicsQuery query) {
        // Ahora devuelve los temas en su orden correcto para el mapa
        return topicRepository.findAllByOrderByTopicOrderAsc();
    }

    @Override
    public List<Lesson> handle(GetLessonsByTopicIdQuery query) {
        //ahora devuleve las lecciones en secuencia
        return lessonRepository.findByTopic_IdOrderByLessonOrderAsc(query.topicId());
    }

    @Override
    public List<Question> handle(GetQuestionsByLessonIdQuery query) {
        return questionRepository.findByLessonId(query.lessonId());
    }

    @Override
    public List<Question> handle(GetRandomQuestionsByLessonIdQuery query) {
        return questionRepository.findRandomQuestionsByLessonId(
                query.lessonId(),
                query.limit()
        );
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
    public List<Question> handle(GetRandomQuestionsQuery query) {
        // 1. Validamos el límite solicitado (fallback seguro de 10 si viene nulo o no positivo)
        int targetLimit = (query != null && query.limit() > 0) ? query.limit() : 10;

        // 2. Obtenemos todos los temas activos ordenados
        List<Topic> topics = topicRepository.findAllByOrderByTopicOrderAsc();

        // 3. Validamos que no este vacio topics
        if (topics.isEmpty()) {
            throw new IllegalStateException("No existen temas registrados en el sistema para construir la evaluación diagnóstica.");
        }

        int totalTopics = topics.size();

        // 4. Algoritmo de partición equitativa (Cociente y Residuo)
        int baseQuestionsPerTopic = targetLimit / totalTopics;
        int residuo = targetLimit % totalTopics;

        List<Question> balancedDiagnostic = new ArrayList<>();

        // 5. Extracción proporcional por cada tema disponible
        for (int i = 0; i < totalTopics; i++) {
            // Los primeros 'residuo' temas absorben 1 pregunta adicional para completar exactamente el targetLimit
            int quotaForThisTopic = baseQuestionsPerTopic + (i < residuo ? 1 : 0);

            if (quotaForThisTopic > 0) {
                UUID topicId = topics.get(i).getId();
                List<Question> questions = questionRepository.findRandomQuestionsByTopic(topicId, quotaForThisTopic);
                balancedDiagnostic.addAll(questions);
            }
        }

        // 6. Se intercala las preguntas
        Collections.shuffle(balancedDiagnostic);

        return balancedDiagnostic;
    }

    @Override
    public Optional<Topic> handle(GetTopicByIdQuery query){
        return topicRepository.findById(query.topicId());
    }

    @Override
    public Optional<Question> handle(GetQuestionByIdQuery query){
        return questionRepository.findById(query.question_id());
    }

    //Nuevo

    @Override
    public List<Question> handle(GetSideQuestQuestionsBySkillQuery query) {
        return questionRepository.findSideQuestQuestionsBySkillAndType(
                query.dktSkillId(),
                "QUIZ",
                query.limit()
        );
    }

    @Override
    public List<Question> handle(GetQuizQuestionsBySkillQuery query) {
        return questionRepository.findRandomQuizQuestionsBySkill(
                query.skillId(),
                query.limit()
        );
    }

    @Override
    public List<Question> handle(GetRandomQuizQuestionsByTopicQuery query) {
        return questionRepository.findRandomQuizQuestionsByTopic(
                query.topicId(),
                query.limit()
        );
    }
}
