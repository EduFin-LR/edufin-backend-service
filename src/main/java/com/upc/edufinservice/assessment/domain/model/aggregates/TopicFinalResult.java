package com.upc.edufinservice.assessment.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "topic_final_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_topic_final_user_topic",
                        columnNames = {"user_id", "topic_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TopicFinalResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "score", nullable = false)
    private Float score;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "time_spent_sec", nullable = false)
    private Integer timeSpentSec;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    public TopicFinalResult(
            UUID userId,
            UUID topicId,
            Float score,
            Boolean passed,
            Integer correctAnswers,
            Integer totalQuestions,
            Integer timeSpentSec
    ) {
        this.userId = userId;
        this.topicId = topicId;
        this.attempts = 0;
        updateResult(
                score,
                passed,
                correctAnswers,
                totalQuestions,
                timeSpentSec
        );
    }

    public void updateResult(
            Float score,
            Boolean passed,
            Integer correctAnswers,
            Integer totalQuestions,
            Integer additionalTimeSpentSec
    ) {
        this.score = score != null ? score : 0.0f;
        this.passed = Boolean.TRUE.equals(passed);
        this.correctAnswers = correctAnswers != null ? correctAnswers : 0;
        this.totalQuestions = totalQuestions != null ? totalQuestions : 0;
        this.timeSpentSec =
                (this.timeSpentSec != null ? this.timeSpentSec : 0)
                        + (additionalTimeSpentSec != null ? additionalTimeSpentSec : 0);
        this.attempts =
                (this.attempts != null ? this.attempts : 0) + 1;
        this.completedAt = Instant.now();
    }
}
