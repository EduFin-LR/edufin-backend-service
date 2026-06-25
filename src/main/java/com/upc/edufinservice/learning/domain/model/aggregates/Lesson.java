package com.upc.edufinservice.learning.domain.model.aggregates;

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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    public Lesson(Topic topic, String title, String content, String videoUrl) {
        this.topic = topic;
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
    }
}