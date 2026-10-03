package com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.learning.domain.model.entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {
    Optional<Skill> findByCode(String code);
    Optional<Skill> findByConceptCode(String conceptCode);
}
