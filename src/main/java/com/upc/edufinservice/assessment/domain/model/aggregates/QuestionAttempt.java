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

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "selected_option_id")
    private UUID selectedOptionId;

    // NUEVO: Guarda el grupo/caja donde el alumno soltó la opción (Para Drag & Drop)
    @Column(name = "selected_match_category")
    private String selectedMatchCategory;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "time_taken_sec")
    private Float timeTakenSec;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;

    // Constructor actualizado para soportar flujos híbridos
    public QuestionAttempt(UUID userId, UUID questionId, UUID selectedOptionId,
                           String selectedMatchCategory, Boolean isCorrect, Float timeTakenSec) {
        this.userId = userId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.selectedMatchCategory = selectedMatchCategory;
        this.isCorrect = isCorrect != null ? isCorrect : false;
        this.timeTakenSec = timeTakenSec;
        this.attemptedAt = LocalDateTime.now();
    }
}