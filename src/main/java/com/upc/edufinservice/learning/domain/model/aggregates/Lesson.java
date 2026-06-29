package com.upc.edufinservice.learning.domain.model.aggregates;

import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación fuerte: Muchas lecciones pueden pertenecer a un solo Tema.
    // Usamos LAZY para que Spring no haga consultas SQL gigantescas innecesarias.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name="lesson_order", nullable = false)
    private Integer lessonOrder;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Enumerated(EnumType.STRING) // NUEVO: Mapeo explícito como String en la BD
    @Column(name = "lesson_type", nullable = false, length = 50)
    private LessonType lessonType;

    public Lesson(Topic topic, Integer lessonOrder, String title, String content, String videoUrl, LessonType lessonType) {
        this.topic = topic;
        this.lessonOrder = lessonOrder;
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.lessonType = lessonType;
    }
}