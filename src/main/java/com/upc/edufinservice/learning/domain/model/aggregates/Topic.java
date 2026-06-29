package com.upc.edufinservice.learning.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "topic_order", nullable = false)
    private Integer topicOrder;

    @Column(length = 50)
    private String category;

    // Esto es para el servicio IA
    @Column(name = "dkt_skill_id")
    private Integer dktSkillId;

    public Topic(String name, String category, Integer dktSkillId) {
        this.name = name;
        this.category = category;
        this.dktSkillId = dktSkillId;
    }
}