package com.upc.edufinservice.analytics.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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
    private Integer isCorrect; // 1 para correcto, 0 para incorrecto

    @Column(name = "interacted_at", nullable = false)
    private LocalDateTime interactedAt;

    public StudentInteraction(UUID userId, Integer dktSkillId, Integer isCorrect) {
        this.userId = userId;
        this.dktSkillId = dktSkillId;
        this.isCorrect = isCorrect;
        this.interactedAt = LocalDateTime.now();
    }
}