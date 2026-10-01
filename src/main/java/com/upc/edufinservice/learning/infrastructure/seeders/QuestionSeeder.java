package com.upc.edufinservice.learning.infrastructure.seeders;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

        System.out.println("⏳ [SEEDER] Cargando nuevo banco estandarizado de preguntas y módulos DKT...");

        ObjectMapper mapper = new ObjectMapper();
        // 1. Configuramos Jackson para ignorar campos de metadatos o investigación del JSON
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 2. Carga flexible: busca 'banco_preguntas_revisado.json' o 'preguntas.json' en classpath
        ClassPathResource resource = new ClassPathResource("data/banco_preguntas_revisado.json");
        if (!resource.exists()) {
            resource = new ClassPathResource("data/preguntas.json");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            TypeReference<List<QuestionSeedDto>> typeReference = new TypeReference<>() {};
            List<QuestionSeedDto> dtos = mapper.readValue(inputStream, typeReference);

            for (QuestionSeedDto dto : dtos) {
                // 3. Buscar o crear el Topic (Módulo) dinámicamente
                String topicName = dto.topicName() != null ? dto.topicName() : "Módulo General";
                Integer topicOrder = dto.topicOrder() != null ? dto.topicOrder() : 1;
                Integer skillId = dto.dktSkillId() != null ? dto.dktSkillId() : 1;

                Topic topic = topicRepository.findByName(topicName)
                        .orElseGet(() -> {
                            Topic t = new Topic(topicName, "Educación Financiera", skillId);
                            t.setTopicOrder(topicOrder);
                            return topicRepository.save(t);
                        });

                // 4. Mapear el LessonType de forma segura
                LessonType currentType;
                try {
                    currentType = (dto.lessonType() != null)
                            ? LessonType.valueOf(dto.lessonType().toUpperCase())
                            : LessonType.QUIZ;
                } catch (IllegalArgumentException e) {
                    currentType = LessonType.QUIZ;
                }

                // 5. Estrategia dinámica de Título y Orden de Lección
                String lessonTitle;
                int lessonOrder;

                if (currentType == LessonType.FINAL) {
                    lessonTitle = "Examen Final: " + topicName;
                    lessonOrder = 10;
                } else {
                    lessonTitle = dto.conceptName() != null ? dto.conceptName() : "Lección " + dto.conceptId();
                    try {
                        lessonOrder = (dto.conceptId() != null && dto.conceptId().contains("."))
                                ? Integer.parseInt(dto.conceptId().split("\\.")[1])
                                : 1;
                    } catch (Exception ex) {
                        lessonOrder = 1;
                    }
                }

                String content = "Práctica interactiva y evaluación sobre " + lessonTitle;
                String videoUrl = "https://sin-video.com";

                // Variables intermedias inmutables para la expresión lambda
                final LessonType finalType = currentType;
                final int finalOrder = lessonOrder;

// 6. Obtención o persistencia de la Lección agrupada
                Lesson lesson = lessonRepository.findByTitleAndLessonTypeAndTopic_Id(lessonTitle, finalType, topic.getId())
                        .orElseGet(() -> lessonRepository.save(new Lesson(
                                topic,
                                finalOrder,
                                lessonTitle,
                                content,
                                videoUrl,
                                finalType,
                                skillId
                        )));

                // 7. Persistencia de la Pregunta
                Question question = new Question(
                        lesson,
                        dto.questionText(),
                        dto.successMessage(), // Se reutiliza feedback_correcto como explicación pedagógica
                        dto.questionType(),
                        dto.hint(),
                        dto.successMessage(),
                        dto.errorMessage(),
                        skillId,
                        null
                );
                questionRepository.save(question);

                // 8. Persistencia de las Opciones (tanto MULTIPLE_CHOICE como DRAG_AND_DROP)
                if (dto.options() != null) {
                    for (OptionSeedDto optionDto : dto.options()) {
                        QuestionOption option = new QuestionOption(
                                question,
                                optionDto.optionText(),
                                optionDto.isCorrect()
                        );
                        option.setMatchCategory(optionDto.matchCategory());
                        questionOptionRepository.save(option);
                    }
                }
            }

            System.out.println("✅ [SEEDER] ¡Banco de 755 preguntas y 43 lecciones registrado exitosamente!");

        } catch (Exception e) {
            System.err.println("❌ [SEEDER] Error procesando las entidades del banco de preguntas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Records DTO con anotaciones @JsonProperty y @JsonAlias para total compatibilidad
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionSeedDto(
            @JsonProperty("modulo") @JsonAlias("topic_name") String topicName,
            @JsonProperty("modulo_orden") @JsonAlias("topic_order") Integer topicOrder,
            @JsonProperty("competencia_dkt") @JsonAlias("dkt_skill_id") Integer dktSkillId,
            @JsonProperty("concepto") String conceptName,
            @JsonProperty("concepto_id") String conceptId,
            @JsonProperty("tipo_leccion") @JsonAlias("lesson_type") String lessonType,
            @JsonProperty("pregunta") @JsonAlias("question_text") String questionText,
            @JsonProperty("tipo_pregunta") @JsonAlias("question_type") String questionType,
            @JsonProperty("pista") @JsonAlias("hint") String hint,
            @JsonProperty("feedback_correcto") @JsonAlias("success_message") String successMessage,
            @JsonProperty("feedback_incorrecto") @JsonAlias("error_message") String errorMessage,
            @JsonProperty("opciones") @JsonAlias("options") List<OptionSeedDto> options
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OptionSeedDto(
            @JsonProperty("texto") @JsonAlias("option_text") String optionText,
            @JsonProperty("es_correcta") @JsonAlias("is_correct") Boolean isCorrect,
            @JsonProperty("categoria") @JsonAlias("match_category") String matchCategory
    ) {}
}