package com.upc.edufinservice.analytics.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "error_patterns")
@Getter
@Setter
@NoArgsConstructor
public class ErrorPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Soft Link al usuario (módulo IAM)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Soft Link al tema (módulo Learning)
    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    // Cuántas veces se ha equivocado en este tema
    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "last_mistake_at")
    private LocalDateTime lastMistakeAt;

    public ErrorPattern(UUID userId, UUID topicId) {
        this.userId = userId;
        this.topicId = topicId;
        this.errorCount = 1; // Inicia en 1 al registrar el primer error
        this.lastMistakeAt = LocalDateTime.now();
    }

    // Lógica de dominio para incrementar el contador
    public void incrementErrorCount() {
        this.errorCount += 1;
        this.lastMistakeAt = LocalDateTime.now();
    }
}