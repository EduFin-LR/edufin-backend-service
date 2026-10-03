package com.upc.edufinservice.analytics.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Última estimación DKT-Forget para una skill concreta de un usuario.
 *
 * Se mantiene una sola fila por user_id + skill_id.
 */
@Entity
@Table(
        name = "skill_mastery_predictions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_skill_mastery_user_skill",
                        columnNames = {"user_id", "skill_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SkillMasteryPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "skill_id", nullable = false)
    private Integer skillId;

    @Column(name = "mastery", nullable = false)
    private Double mastery;

    @Column(name = "predicted_at", nullable = false)
    private Instant predictedAt;

    public SkillMasteryPrediction(
            UUID userId,
            Integer skillId,
            Double mastery
    ) {
        this.userId = userId;
        this.skillId = skillId;
        updateMastery(mastery);
    }

    public void updateMastery(Double mastery) {
        if (mastery == null || mastery < 0.0 || mastery > 1.0) {
            throw new IllegalArgumentException(
                    "mastery debe estar entre 0.0 y 1.0"
            );
        }

        this.mastery = mastery;
        this.predictedAt = Instant.now();
    }
}
