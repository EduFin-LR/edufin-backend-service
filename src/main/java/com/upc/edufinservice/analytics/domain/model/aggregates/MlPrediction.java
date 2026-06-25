package com.upc.edufinservice.analytics.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ml_predictions")
@Getter
@Setter
@NoArgsConstructor
public class MlPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    // Probabilidad de éxito devuelta por DKT (ej. 0.85)
    @Column(name = "predicted_success_probability")
    private Float predictedSuccessProbability;

    // Soft Link a la lección que la IA recomienda estudiar a continuación
    @Column(name = "recommended_lesson_id")
    private UUID recommendedLessonId;

    @Column(name = "predicted_at")
    private LocalDateTime predictedAt;

    public MlPrediction(UUID userId, UUID topicId, Float predictedSuccessProbability, UUID recommendedLessonId) {
        this.userId = userId;
        this.topicId = topicId;
        this.predictedSuccessProbability = predictedSuccessProbability;
        this.recommendedLessonId = recommendedLessonId;
        this.predictedAt = LocalDateTime.now();
    }

    public void updatePrediction(Float newProbability, UUID newRecommendedLessonId) {
        this.predictedSuccessProbability = newProbability;
        this.recommendedLessonId = newRecommendedLessonId;
        this.predictedAt = LocalDateTime.now();
    }
}