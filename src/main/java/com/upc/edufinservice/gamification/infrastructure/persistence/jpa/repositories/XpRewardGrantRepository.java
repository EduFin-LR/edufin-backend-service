package com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.gamification.domain.model.aggregates.XpRewardGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface XpRewardGrantRepository extends JpaRepository<XpRewardGrant, UUID> {

    // PostgreSQL: la inserción y la comprobación de duplicados son atómicas.
    // Un SELECT previo no protege contra dos solicitudes simultáneas.
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO xp_reward_grants (id, user_id, source_id, reward_type, points, created_at)
            VALUES (:id, :userId, :sourceId, :rewardType, :points, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, reward_type, source_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("sourceId") UUID sourceId,
            @Param("rewardType") String rewardType,
            @Param("points") int points
    );
}
