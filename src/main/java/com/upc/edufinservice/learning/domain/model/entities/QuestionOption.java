package com.upc.edufinservice.learning.domain.model.entities;

import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación fuerte con Question
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false, columnDefinition = "text")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    public QuestionOption(Question question, String optionText, Boolean isCorrect) {
        this.question = question;
        this.optionText = optionText;
        this.isCorrect = isCorrect != null ? isCorrect : false;
    }
}