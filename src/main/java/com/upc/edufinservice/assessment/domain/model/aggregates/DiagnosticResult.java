package com.upc.edufinservice.assessment.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "diagnostic_results")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Soft Link al módulo IAM
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // Nota del 0 al 100 según el script SQL
    @Column(name = "score", nullable = false)
    private Float score;

    public DiagnosticResult(UUID userId, Float score) {
        this.userId = userId;
        this.score = score;
    }
}