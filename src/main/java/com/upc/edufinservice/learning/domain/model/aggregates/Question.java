package com.upc.edufinservice.learning.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación fuerte con Lesson
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    private String questionText;

    @Column(columnDefinition = "text")
    private String explanation;

    public Question(Lesson lesson, String questionText, String explanation) {
        this.lesson = lesson;
        this.questionText = questionText;
        this.explanation = explanation;
    }
}