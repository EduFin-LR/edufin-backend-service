package com.upc.edufinservice.assessment.domain.model.experimental;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "experimental_assessment_responses",
       uniqueConstraints = @UniqueConstraint(name = "uk_experimental_response_session_question", columnNames = {"session_id", "question_id"}))
@Getter
@NoArgsConstructor
public class ExperimentalAssessmentResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ExperimentalAssessmentSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ExperimentalQuestion question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_option_id", nullable = false)
    private ExperimentalOption selectedOption;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "time_taken_sec", nullable = false)
    private Float timeTakenSec;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    public ExperimentalAssessmentResponse(ExperimentalAssessmentSession session, ExperimentalQuestion question,
                                          ExperimentalOption selectedOption, Boolean isCorrect, Float timeTakenSec) {
        this.session = session;
        this.question = question;
        this.selectedOption = selectedOption;
        this.isCorrect = isCorrect;
        this.timeTakenSec = timeTakenSec != null ? timeTakenSec : 0.0f;
        this.answeredAt = Instant.now();
    }
}
