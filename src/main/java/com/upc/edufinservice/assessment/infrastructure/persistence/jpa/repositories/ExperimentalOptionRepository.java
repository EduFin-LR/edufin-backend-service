package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExperimentalOptionRepository extends JpaRepository<ExperimentalOption, UUID> {
    Optional<ExperimentalOption> findByCode(String code);
}
