package com.upc.edufinservice.learning.infrastructure.seeders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
// Asegúrate de importar tus repositorios correctamente
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.LessonRepository;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.QuestionOptionRepository;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.QuestionRepository;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.TopicRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class QuestionSeeder implements CommandLineRunner {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    public QuestionSeeder(TopicRepository topicRepository,
                          LessonRepository lessonRepository,
                          QuestionRepository questionRepository,
                          QuestionOptionRepository questionOptionRepository) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Evitamos que se dupliquen los datos si el servidor se reinicia
        if (questionRepository.count() > 0) {
            System.out.println("✅ [SEEDER] Las preguntas ya existen en la base de datos. Seeder omitido.");
            return;
        }

        System.out.println("⏳ [SEEDER] Cargando banco de preguntas y tópicos...");

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<List<QuestionSeedDto>> typeReference = new TypeReference<>() {};
        InputStream inputStream = new ClassPathResource("data/preguntas.json").getInputStream();

        try {
            List<QuestionSeedDto> dtos = mapper.readValue(inputStream, typeReference);

            for (QuestionSeedDto dto : dtos) {
                // 1. Buscar o crear el Topic (Unidad)
                Topic topic = topicRepository.findByName(dto.topic_name())
                        .orElseGet(() -> topicRepository.save(new Topic(dto.topic_name(), dto.topic_category(), dto.dkt_skill_id())));

                // 2. Buscar o crear la Lesson (Lección)
                Lesson lesson = lessonRepository.findByTitleAndTopicId(dto.lesson_title(), topic.getId())
                        .orElseGet(() -> lessonRepository.save(new Lesson(topic, dto.lesson_title(), "Contenido pendiente", "url_pendiente")));

                // 3. Crear la Pregunta
                Question question = new Question(lesson, dto.question_text(), dto.explanation());
                questionRepository.save(question);

                // 4. Crear las Opciones de la pregunta
                for (OptionSeedDto optionDto : dto.options()) {
                    QuestionOption option = new QuestionOption(question, optionDto.option_text(), optionDto.is_correct());
                    questionOptionRepository.save(option);
                }
            }
            System.out.println("✅ [SEEDER] ¡Banco de preguntas, lecciones y tópicos inyectados con éxito!");

        } catch (Exception e) {
            System.err.println("❌ [SEEDER] Error leyendo el archivo preguntas.json: " + e.getMessage());
        }
    }

    // DTOs internos (Records) para mapear el JSON automáticamente
    public record QuestionSeedDto(
            String topic_name,
            String topic_category,
            Integer dkt_skill_id,
            String lesson_title,
            String question_text,
            String explanation,
            List<OptionSeedDto> options
    ) {}

    public record OptionSeedDto(
            String option_text,
            Boolean is_correct
    ) {}
}