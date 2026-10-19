package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

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

    @Query(
            value = """
                SELECT DISTINCT ON (user_id, skill_id) *
                FROM skill_mastery_history
                ORDER BY user_id, skill_id, recorded_at DESC
                """,
            nativeQuery = true
    )
    List<SkillMasteryHistory> findLatestMasteryByUserAndSkill();
}
