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

    @Column(name="question_type", nullable = false)
    private String questionType;

    @Column(name="hint", columnDefinition = "TEXT")
    private String hint;

    @Column(name="success_message", columnDefinition = "TEXT")
    private String successMessage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "question_text", columnDefinition = "text")
    private String questionText;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "dkt_skill_id")
    private Integer dktSkillId;

    @Column(name = "theory_text", columnDefinition = "text")
    private String theoryText;

    public Question(Lesson lesson, String questionText, String explanation, String questionType,
                    String hint, String successMessage, String errorMessage, Integer dktSkillId,
                    String theoryText) {
        this.lesson = lesson;
        this.questionText = questionText;
        this.explanation = explanation;
        this.questionType = questionType;
        this.hint = hint;
        this.successMessage = successMessage;
        this.errorMessage = errorMessage;
        this.dktSkillId = dktSkillId;
        this.theoryText = theoryText; // Almacena la teoría específica de esta diapositiva
    }
}