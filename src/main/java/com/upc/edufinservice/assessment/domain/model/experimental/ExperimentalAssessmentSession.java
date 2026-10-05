package com.upc.edufinservice.assessment.domain.model.experimental;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "experimental_assessment_sessions",
       uniqueConstraints = @UniqueConstraint(name = "uk_experimental_session_user_phase", columnNames = {"user_id", "phase"}))
@Getter
@NoArgsConstructor
public class ExperimentalAssessmentSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperimentalAssessmentPhase phase;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    @Column(name = "score_percent", nullable = false)
    private Float scorePercent;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public ExperimentalAssessmentSession(UUID userId, ExperimentalAssessmentPhase phase,
                                         Integer totalQuestions, Integer correctAnswers, Float scorePercent) {
        this.userId = userId;
        this.phase = phase;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.scorePercent = scorePercent;
        this.submittedAt = Instant.now();
    }
}
