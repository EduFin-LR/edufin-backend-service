package com.upc.edufinservice.assessment.domain.model.experimental;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "experimental_questions", uniqueConstraints = @UniqueConstraint(name = "uk_experimental_question_code", columnNames = "code"))
@Getter
@NoArgsConstructor
public class ExperimentalQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;

    @Column(name = "module_name", nullable = false, length = 120)
    private String moduleName;

    @Column(name = "competency", nullable = false, length = 180)
    private String competency;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    /**
     * Skill estable 1..30 utilizada únicamente para inicializar DKT desde el PRE_TEST.
     * Se deja nullable a nivel de esquema para permitir la migración de bancos ya existentes;
     * el seeder la completa y el servicio valida que exista antes de enviar datos al modelo.
     */
    @Column(name = "dkt_skill_id")
    private Integer dktSkillId;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("optionOrder ASC")
    private List<ExperimentalOption> options = new ArrayList<>();

    public ExperimentalQuestion(String code, Integer questionOrder, Integer moduleNumber, String moduleName,
                                String competency, String questionText, String sourceReference, Integer dktSkillId) {
        this.code = code;
        this.questionOrder = questionOrder;
        this.moduleNumber = moduleNumber;
        this.moduleName = moduleName;
        this.competency = competency;
        this.questionText = questionText;
        this.sourceReference = sourceReference;
        this.dktSkillId = dktSkillId;
    }

    public void updateDktSkillId(Integer dktSkillId) {
        this.dktSkillId = dktSkillId;
    }

    public void addOption(ExperimentalOption option) {
        options.add(option);
        option.attachTo(this);
    }
}
