package com.upc.edufinservice.learning.infrastructure.seeders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
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
        if (questionRepository.count() > 0) {
            System.out.println("✅ [SEEDER] Las preguntas ya existen en la base de datos. Seeder omitido.");
            return;
        }

        System.out.println("⏳ [SEEDER] Cargando banco de 120 preguntas gamificadas y secuenciales...");

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<List<QuestionSeedDto>> typeReference = new TypeReference<>() {};
        InputStream inputStream = new ClassPathResource("data/preguntas.json").getInputStream();

        try {
            List<QuestionSeedDto> dtos = mapper.readValue(inputStream, typeReference);

            for (QuestionSeedDto dto : dtos) {
                // 1. Buscar o crear el Topic (Unidad)
                Topic topic = topicRepository.findByName(dto.topic_name())
                        .orElseGet(() -> {
                            Topic t = new Topic(dto.topic_name(), "Educación Financiera", dto.dkt_skill_id());
                            t.setTopicOrder(dto.topic_order());
                            return topicRepository.save(t);
                        });

                // 2. Mapear el LessonType de String (JSON) a tu Enum de JPA
                LessonType currentType = LessonType.valueOf(dto.lesson_type());

                // 🔥 CORRECCIÓN 2: Lógica dinámica para guardar Teoría o Descripción de Video en el campo "content"
                String content = dto.theory_text() != null ? dto.theory_text() :
                        (dto.video_description() != null ? dto.video_description() : "Contenido interactivo");

                String videoUrl = dto.video_url() != null ? dto.video_url() : "https://sin-video.com";

                // 🔥 CORRECCIÓN 3 y 4: Búsqueda con Topic_Id e inyección del dktSkillId en Lesson
                Lesson lesson = lessonRepository.findByTitleAndLessonTypeAndTopic_Id(dto.lesson_title(), currentType, topic.getId())
                        .orElseGet(() -> lessonRepository.save(new Lesson(
                                topic,
                                dto.lesson_order(),
                                dto.lesson_title(),
                                content,
                                videoUrl,
                                currentType,
                                dto.dkt_skill_id() // ¡IA tracking activado para lecciones y videos!
                        )));

                // Si la estación es de tipo VIDEO, no mapea preguntas ni opciones
                if (currentType == LessonType.VIDEO) {
                    continue;
                }

                // 🔥 CORRECCIÓN 4: Inyección del dktSkillId en las Question
                Question question = new Question(
                        lesson,
                        dto.question_text(),
                        dto.explanation(),
                        dto.question_type(),
                        dto.hint(),
                        dto.success_message(),
                        dto.error_message(),
                        dto.dkt_skill_id(),
                        dto.theory_text()// ¡IA tracking activado para cada pregunta!
                );
                questionRepository.save(question);

                // Crear las Opciones de la pregunta (si existen)
                if (dto.options() != null) {
                    for (OptionSeedDto optionDto : dto.options()) {
                        QuestionOption option = new QuestionOption(question, optionDto.option_text(), optionDto.is_correct());
                        option.setMatchCategory(optionDto.match_category());
                        questionOptionRepository.save(option);
                    }
                }
            }
            System.out.println("✅ [SEEDER] ¡La arquitectura unificada con IA se ha guardado exitosamente!");

        } catch (Exception e) {
            System.err.println("❌ [SEEDER] Error procesando las entidades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // DTOs internos expandidos para mapear el 100% de las llaves del JSON nuevo
    public record QuestionSeedDto(
            String topic_name,
            Integer topic_order,
            Integer dkt_skill_id,
            String lesson_title,
            Integer lesson_order,
            String lesson_type,
            String theory_text,
            String video_url,
            String video_description,
            String question_text,
            String question_type,
            String hint,
            String success_message,
            String error_message,
            String explanation,
            List<OptionSeedDto> options
    ) {}

    public record OptionSeedDto(
            String option_text,
            Boolean is_correct,
            String match_category
    ) {}
}