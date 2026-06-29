package com.upc.edufinservice.learning.domain.model.entities;

import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_lesson_progress")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Crea el constructor vacío que exige JPA automáticamente
public class UserLessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProgressStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "score", nullable = false)
    private Float score;

    @Column(name = "time_spent_sec", nullable = false)
    private Integer timeSpentSec;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    // ========================================================================
    // CONSTRUCTOR PERSONALIZADO (Para cuando el alumno inicia la lección)
    // ========================================================================
    public UserLessonProgress(UUID userId, Lesson lesson) {
        this.userId = userId;
        this.lesson = lesson;
        this.status = ProgressStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.score = 0.0f;
        this.timeSpentSec = 0;
        this.attempts = 0;
    }

    // ========================================================================
    // MÉTODO DE DOMINIO (Para marcar la lección como terminada)
    // ========================================================================
    public void markAsCompleted(Float finalScore, Integer additionalTimeSpent) {
        this.status = ProgressStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        // Regla de negocio: Solo actualizamos el score si el nuevo es mayor
        if (finalScore != null && finalScore > this.score) {
            this.score = finalScore;
        }

        this.timeSpentSec += additionalTimeSpent;
        this.attempts += 1;
    }
}