package com.upc.edufinservice.gamification.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historial idempotente de recompensas.
 * La restricción única impide premiar dos veces una misma fuente.
 */
@Entity
@Table(name = "xp_reward_grants", uniqueConstraints = @UniqueConstraint(
        name = "uk_xp_reward_user_type_source",
        columnNames = {"user_id", "reward_type", "source_id"}
))
@Getter
public class XpRewardGrant {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "reward_type", nullable = false, length = 32)
    private String rewardType;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected XpRewardGrant() { }
}
