package com.upc.edufinservice.learning.domain.model.aggregates;

import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.entities.Skill;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "lesson_order", nullable = false)
    private Integer lessonOrder;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 50)
    private LessonType lessonType;

    public Lesson(
            Topic topic,
            Skill skill,
            Integer lessonOrder,
            String title,
            String content,
            String videoUrl,
            LessonType lessonType
    ) {
        this.topic = topic;
        this.skill = skill;
        this.lessonOrder = lessonOrder;
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.lessonType = lessonType;
    }
}