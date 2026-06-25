package com.upc.edufinservice.learning.domain.model.entities;

import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_lesson_progress")
@Getter
@Setter
@NoArgsConstructor
public class UserLessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Relación directa con la tabla LESSONS como dicta tu script SQL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "score")
    private Float score = 0.0f;

    @Column(name = "time_spent_sec")
    private Integer timeSpentSec = 0;

    @Column(name = "attempts")
    private Integer attempts = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Constructor para cuando el usuario inicia una lección por primera vez
    public UserLessonProgress(UUID userId, Lesson lesson) {
        this.userId = userId;
        this.lesson = lesson;
        this.startedAt = LocalDateTime.now();
        this.completed = false;
        this.score = 0.0f;
        this.timeSpentSec = 0;
        this.attempts = 0;
    }

    // Método de dominio para marcar la lección como completada
    public void markAsCompleted(Float finalScore, Integer additionalTimeSpent) {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
        this.score = finalScore;
        this.timeSpentSec += additionalTimeSpent;
        this.attempts += 1;
    }
}