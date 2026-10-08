package com.upc.edufinservice.analytics.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Punto histórico de mastery para una skill de un usuario.
 *
 * A diferencia de SkillMasteryPrediction, aquí no se actualiza una fila existente:
 * cada snapshot relevante genera una nueva fila para poder reconstruir la evolución.
 */
@Entity
@Table(
        name = "skill_mastery_history",
        indexes = {
                @Index(name = "idx_mastery_history_user_skill_time", columnList = "user_id,skill_id,recorded_at"),
                @Index(name = "idx_mastery_history_user_time", columnList = "user_id,recorded_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SkillMasteryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "skill_id", nullable = false)
    private Integer skillId;

    @Column(name = "mastery", nullable = false)
    private Double mastery;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "interaction_count", nullable = false)
    private Long interactionCount;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_source", nullable = false, length = 30)
    private MasterySnapshotSource snapshotSource;

    @Column(name = "context_id")
    private UUID contextId;

    public SkillMasteryHistory(
            UUID userId,
            Integer skillId,
            Double mastery,
            Instant recordedAt,
            Long interactionCount,
            String modelVersion,
            MasterySnapshotSource snapshotSource,
            UUID contextId
    ) {
        if (mastery == null || mastery < 0.0 || mastery > 1.0) {
            throw new IllegalArgumentException("mastery debe estar entre 0.0 y 1.0");
        }

        this.userId = userId;
        this.skillId = skillId;
        this.mastery = mastery;
        this.recordedAt = recordedAt;
        this.interactionCount = interactionCount;
        this.modelVersion = modelVersion;
        this.snapshotSource = snapshotSource;
        this.contextId = contextId;
    }
}
