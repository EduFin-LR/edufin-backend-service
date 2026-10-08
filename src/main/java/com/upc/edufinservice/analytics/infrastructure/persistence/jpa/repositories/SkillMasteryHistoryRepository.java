package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillMasteryHistoryRepository
        extends JpaRepository<SkillMasteryHistory, UUID> {

    List<SkillMasteryHistory> findByUserIdOrderByRecordedAtAsc(UUID userId);

    List<SkillMasteryHistory> findByUserIdAndSkillIdOrderByRecordedAtAsc(
            UUID userId,
            Integer skillId
    );
}
