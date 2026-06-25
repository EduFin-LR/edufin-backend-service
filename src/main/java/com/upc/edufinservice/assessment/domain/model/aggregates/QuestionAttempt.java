package com.upc.edufinservice.assessment.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "question_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Soft Link hacia el módulo IAM
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Soft Link hacia el módulo Learning
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    // Soft Link hacia la opción elegida en Learning
    @Column(name = "selected_option_id")
    private UUID selectedOptionId;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "time_taken_sec")
    private Float timeTakenSec;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;

    public QuestionAttempt(UUID userId, UUID questionId, UUID selectedOptionId, Boolean isCorrect, Float timeTakenSec) {
        this.userId = userId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.isCorrect = isCorrect != null ? isCorrect : false;
        this.timeTakenSec = timeTakenSec;
        this.attemptedAt = LocalDateTime.now();
    }
}