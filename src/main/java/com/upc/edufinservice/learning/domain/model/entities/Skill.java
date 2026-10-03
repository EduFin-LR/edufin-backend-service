package com.upc.edufinservice.learning.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
public class Skill {

    /**
     * ID estable utilizado también por DKT/DKT-Forget.
     *
     * IMPORTANTE:
     * No usar @GeneratedValue. Los IDs 1..30 forman parte del contrato
     * entre la base de datos, Spring Boot y el servicio de ML.
     */
    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "concept_code", nullable = false, unique = true, length = 10)
    private String conceptCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "module_order", nullable = false)
    private Integer moduleOrder;

    public Skill(
            Integer id,
            String code,
            String conceptCode,
            String name,
            Integer moduleOrder
    ) {
        this.id = id;
        this.code = code;
        this.conceptCode = conceptCode;
        this.name = name;
        this.moduleOrder = moduleOrder;
    }
}
