package com.upc.edufinservice.analytics.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_interactions")
@Getter
@Setter
@NoArgsConstructor
public class StudentInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "dkt_skill_id", nullable = false)
    private Integer dktSkillId;

    @Column(name = "is_correct", nullable = false)
    private Integer isCorrect; // 1 correcto, 0 incorrecto

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 30)
    private InteractionType interactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_reason", nullable = false, length = 30)
    private SelectionReason selectionReason;

    @Column(name = "interacted_at", nullable = false)
    private Instant interactedAt;

    public StudentInteraction(
            UUID userId,
            Integer dktSkillId,
            Integer isCorrect,
            InteractionType interactionType,
            SelectionReason selectionReason
    ) {
        this.userId = userId;
        this.dktSkillId = dktSkillId;
        this.isCorrect = isCorrect;
        this.interactionType = interactionType;
        this.selectionReason = selectionReason;
        this.interactedAt = Instant.now();
    }
}
