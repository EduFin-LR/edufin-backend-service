package com.upc.edufinservice.assessment.domain.model.aggregates;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
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

    @Column(name = "selected_match_category")
    private String selectedMatchCategory;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "time_taken_sec")
    private Float timeTakenSec;

    /*
     * Metadatos pedagógicos de cómo fue presentada la pregunta.
     *
     * Se permiten null para mantener compatibilidad con intentos antiguos.
     * StudentInteraction resolverá un fallback si son null.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", length = 30)
    private InteractionType interactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_reason", length = 30)
    private SelectionReason selectionReason;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;

    public QuestionAttempt(
            UUID userId,
            UUID questionId,
            UUID selectedOptionId,
            String selectedMatchCategory,
            Boolean isCorrect,
            Float timeTakenSec,
            InteractionType interactionType,
            SelectionReason selectionReason
    ) {
        this.userId = userId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.selectedMatchCategory = selectedMatchCategory;
        this.isCorrect = isCorrect != null ? isCorrect : false;
        this.timeTakenSec = timeTakenSec;
        this.interactionType = interactionType;
        this.selectionReason = selectionReason;
        this.attemptedAt = LocalDateTime.now();
    }
}
