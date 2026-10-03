package com.upc.edufinservice.learning.infrastructure.seeders;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.QuestionDifficulty;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
import com.upc.edufinservice.learning.domain.model.entities.Skill;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.LessonRepository;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.QuestionOptionRepository;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.QuestionRepository;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.SkillRepository;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.TopicRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@Order(2)
public class QuestionSeeder implements CommandLineRunner {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SkillRepository skillRepository;

    public QuestionSeeder(
            TopicRepository topicRepository,
            LessonRepository lessonRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository questionOptionRepository,
            SkillRepository skillRepository
    ) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (questionRepository.count() > 0) {
            System.out.println("✅ [QUESTION SEEDER] Las preguntas ya existen. Seeder omitido.");
            return;
        }

        System.out.println("⏳ [QUESTION SEEDER] Cargando banco de preguntas...");

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ClassPathResource resource =
                new ClassPathResource("data/banco_preguntas_estructurado.json");

        if (!resource.exists()) {
            resource = new ClassPathResource("data/banco_preguntas_revisado.json");
        }

        if (!resource.exists()) {
            resource = new ClassPathResource("data/preguntas.json");
        }

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "No se encontró ningún banco de preguntas en src/main/resources/data/"
            );
        }

        System.out.println("📄 [QUESTION SEEDER] Archivo utilizado: " + resource.getPath());

        try (InputStream inputStream = resource.getInputStream()) {

            TypeReference<List<QuestionSeedDto>> typeReference = new TypeReference<>() {};
            List<QuestionSeedDto> dtos = mapper.readValue(inputStream, typeReference);

            int lessonsCreated = 0;
            int questionsCreated = 0;
            int optionsCreated = 0;
            int finalsIgnored = 0;

            for (QuestionSeedDto dto : dtos) {

                LessonType lessonType = parseLessonType(dto.lessonType());

                /*
                 * El FINAL será generado dinámicamente cuando se solicite.
                 * Por eso no se persisten lecciones/preguntas FINAL desde el banco.
                 */
                if (lessonType == LessonType.FINAL) {
                    finalsIgnored++;
                    continue;
                }

                if (dto.dktSkillId() == null) {
                    throw new IllegalStateException(
                            "La pregunta " + safeId(dto.sourceId())
                                    + " no tiene competencia_dkt."
                    );
                }

                if (dto.questionText() == null || dto.questionText().isBlank()) {
                    throw new IllegalStateException(
                            "La pregunta " + safeId(dto.sourceId())
                                    + " no tiene texto."
                    );
                }

                /*
                 * 1. Skill
                 */
                final Integer skillId = dto.dktSkillId();

                final Skill skill = skillRepository.findById(skillId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe la Skill con id " + skillId
                                        + ". Asegúrate de ejecutar SkillSeeder antes que QuestionSeeder."
                        ));

                /*
                 * 2. Topic / módulo
                 *
                 * Topic ya no contiene skill.
                 */
                String topicName = dto.topicName() != null && !dto.topicName().isBlank()
                        ? dto.topicName()
                        : "Módulo General";

                Integer topicOrder = dto.topicOrder() != null
                        ? dto.topicOrder()
                        : skill.getModuleOrder();

                Topic topic = topicRepository.findByName(topicName)
                        .orElseGet(() -> {
                            Topic newTopic = new Topic(
                                    topicName,
                                    "Educación Financiera"
                            );
                            newTopic.setTopicOrder(topicOrder);
                            return topicRepository.save(newTopic);
                        });

                /*
                 * 3. Lesson
                 *
                 * En el banco nuevo, LESSON y QUIZ se agrupan por concepto.
                 * Son lecciones distintas porque lesson_type forma parte de la búsqueda.
                 */
                String lessonTitle = dto.conceptName() != null && !dto.conceptName().isBlank()
                        ? dto.conceptName()
                        : "Concepto " + safeId(dto.conceptId());

                int lessonOrder = extractConceptOrder(dto.conceptId());

                String lessonContent = switch (lessonType) {
                    case LESSON -> "Contenido teórico y práctica sobre " + lessonTitle;
                    case QUIZ -> "Evaluación sobre " + lessonTitle;
                    case VIDEO -> "Material audiovisual sobre " + lessonTitle;
                    case FINAL -> throw new IllegalStateException(
                            "FINAL no debería llegar a creación de Lesson."
                    );
                };

                String videoUrl = dto.videoUrl();

                final LessonType finalLessonType = lessonType;
                final int finalLessonOrder = lessonOrder;
                final String finalLessonContent = lessonContent;
                final String finalVideoUrl = videoUrl;

                boolean lessonAlreadyExists =
                        lessonRepository.findByTitleAndLessonTypeAndTopic_Id(
                                lessonTitle,
                                finalLessonType,
                                topic.getId()
                        ).isPresent();

                Lesson lesson = lessonRepository
                        .findByTitleAndLessonTypeAndTopic_Id(
                                lessonTitle,
                                finalLessonType,
                                topic.getId()
                        )
                        .orElseGet(() -> lessonRepository.save(
                                new Lesson(
                                        topic,
                                        skill,
                                        finalLessonOrder,
                                        lessonTitle,
                                        finalLessonContent,
                                        finalVideoUrl,
                                        finalLessonType
                                )
                        ));

                if (!lessonAlreadyExists) {
                    lessonsCreated++;
                }

                /*
                 * 4. Teoría
                 *
                 * Cada registro LESSON puede tener su propio contenido teórico.
                 * Se conserva en Question.theoryText como JSON serializado.
                 *
                 * Para QUIZ normalmente será null.
                 */
                String theoryText = null;

                if (lessonType == LessonType.LESSON && dto.lessonContent() != null) {
                    theoryText = mapper.writeValueAsString(dto.lessonContent());
                }

                /*
                 * 5. Question
                 *
                 * Question conserva Skill porque es la unidad que finalmente
                 * genera una interacción para DKT/DKT-Forget.
                 */
                QuestionDifficulty difficulty = parseDifficulty(dto.difficulty());
                Question question = new Question(
                        lesson,
                        skill,
                        dto.questionText(),
                        dto.successMessage(), // explicación pedagógica actual
                        dto.questionType(),
                        dto.hint(),
                        dto.successMessage(),
                        dto.errorMessage(),
                        theoryText,
                        difficulty
                );

                questionRepository.save(question);
                questionsCreated++;

                /*
                 * 6. Opciones
                 */
                if (dto.options() != null) {
                    for (OptionSeedDto optionDto : dto.options()) {

                        if (optionDto.optionText() == null
                                || optionDto.optionText().isBlank()) {
                            continue;
                        }

                        QuestionOption option = new QuestionOption(
                                question,
                                optionDto.optionText(),
                                optionDto.isCorrect()
                        );

                        option.setMatchCategory(optionDto.matchCategory());

                        questionOptionRepository.save(option);
                        optionsCreated++;
                    }
                }
            }

            System.out.println(
                    "✅ [QUESTION SEEDER] Carga terminada. "
                            + lessonsCreated + " lecciones creadas, "
                            + questionsCreated + " preguntas creadas, "
                            + optionsCreated + " opciones creadas, "
                            + finalsIgnored + " registros FINAL ignorados."
            );

        } catch (Exception e) {
            System.err.println(
                    "❌ [QUESTION SEEDER] Error procesando el banco: "
                            + e.getMessage()
            );
            e.printStackTrace();
            throw e;
        }
    }

    private LessonType parseLessonType(String value) {
        if (value == null || value.isBlank()) {
            return LessonType.QUIZ;
        }

        try {
            return LessonType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            System.out.println(
                    "⚠️ [QUESTION SEEDER] LessonType desconocido '"
                            + value + "'. Se usará QUIZ."
            );
            return LessonType.QUIZ;
        }
    }

    private int extractConceptOrder(String conceptId) {
        if (conceptId == null || conceptId.isBlank()) {
            return 1;
        }

        try {
            String[] parts = conceptId.split("\\.");

            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }

            return Integer.parseInt(parts[0]);

        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private static String safeId(String value) {
        return value != null ? value : "(sin id)";
    }

    private QuestionDifficulty parseDifficulty(Integer value) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case 1 -> QuestionDifficulty.BASIC;
            case 2 -> QuestionDifficulty.INTERMEDIATE;
            case 3 -> QuestionDifficulty.ADVANCED;

            default -> throw new IllegalArgumentException(
                    "Dificultad desconocida: " + value
            );
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionSeedDto(

            @JsonProperty("id")
            String sourceId,

            @JsonProperty("modulo")
            @JsonAlias("topic_name")
            String topicName,

            @JsonProperty("modulo_orden")
            @JsonAlias("topic_order")
            Integer topicOrder,

            @JsonProperty("competencia_dkt")
            @JsonAlias("dkt_skill_id")
            Integer dktSkillId,

            @JsonProperty("concepto")
            String conceptName,

            @JsonProperty("concepto_id")
            String conceptId,

            @JsonProperty("orden_en_concepto")
            Integer conceptOrder,

            @JsonProperty("tipo_leccion")
            @JsonAlias("lesson_type")
            String lessonType,

            @JsonProperty("contenido_leccion")
            LessonContentSeedDto lessonContent,

            @JsonProperty("pregunta")
            @JsonAlias("question_text")
            String questionText,

            @JsonProperty("tipo_pregunta")
            @JsonAlias("question_type")
            String questionType,

            @JsonProperty("pista")
            @JsonAlias("hint")
            String hint,

            @JsonProperty("feedback_correcto")
            @JsonAlias("success_message")
            String successMessage,

            @JsonProperty("feedback_incorrecto")
            @JsonAlias("error_message")
            String errorMessage,

            @JsonProperty("opciones")
            @JsonAlias("options")
            List<OptionSeedDto> options,

            @JsonProperty("video_url")
            String videoUrl,

            @JsonProperty("dificultad")
            Integer difficulty
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LessonContentSeedDto(

            @JsonProperty("titulo")
            String title,

            @JsonProperty("texto")
            String text,

            @JsonProperty("ejemplo")
            String example,

            @JsonProperty("idea_clave")
            String keyIdea
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OptionSeedDto(

            @JsonProperty("texto")
            @JsonAlias("option_text")
            String optionText,

            @JsonProperty("es_correcta")
            @JsonAlias("is_correct")
            Boolean isCorrect,

            @JsonProperty("categoria")
            @JsonAlias("match_category")
            String matchCategory
    ) {}
}
