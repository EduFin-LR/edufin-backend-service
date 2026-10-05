package com.upc.edufinservice.assessment.domain.model.experimental;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "experimental_options", uniqueConstraints = @UniqueConstraint(name = "uk_experimental_option_code", columnNames = "code"))
@Getter
@NoArgsConstructor
public class ExperimentalOption {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ExperimentalQuestion question;

    @Column(name = "option_order", nullable = false)
    private Integer optionOrder;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    public ExperimentalOption(String code, Integer optionOrder, String optionText, Boolean isCorrect) {
        this.code = code;
        this.optionOrder = optionOrder;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }

    void attachTo(ExperimentalQuestion question) {
        this.question = question;
    }
}
