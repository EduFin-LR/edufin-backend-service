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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
       // 1. Obtenemos la lista de temas ordenados por su orden oficial (Topic 1, Topic 2...)
       List<Topic> topics = topicRepository.findAllByOrderByTopicOrderAsc();

       // Control de seguridad por si la base de datos no está poblada completamente
       if (topics.size() < 2) {
           return questionRepository.findRandomQuestions(query.limit());
       }

       // 2. Extraemos los identificadores únicos del Tema 1 y Tema 2
       UUID topic1Id = topics.get(0).getId();
       UUID topic2Id = topics.get(1).getId();

       // 3. Jalamos exactamente 5 preguntas aleatorias de cada competencia
       List<Question> topic1Questions = questionRepository.findRandomQuestionsByTopic(topic1Id, 5);
       List<Question> topic2Questions = questionRepository.findRandomQuestionsByTopic(topic2Id, 5);

       // 4. Consolidamos ambos bloques en una sola lista balanceada de 10 ejercicios
       List<Question> balancedDiagnostic = new ArrayList<>();
       balancedDiagnostic.addAll(topic1Questions);
       balancedDiagnostic.addAll(topic2Questions);

       return balancedDiagnostic;
   }
   
   //Nuevo
    @Override
    public Optional<Topic> handle(GetTopicByIdQuery query){
        return topicRepository.findById(query.topicId());
    }
}