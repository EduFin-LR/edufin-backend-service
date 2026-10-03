package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.entities.StudentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentInteractionRepository extends JpaRepository<StudentInteraction, UUID> {

    List<StudentInteraction> findByUserIdOrderByInteractedAtAsc(UUID userId);

    @Query("""
            SELECT DISTINCT si.dktSkillId
            FROM StudentInteraction si
            WHERE si.userId = :userId
            """)
    List<Integer> findDistinctSkillIdsByUserId(@Param("userId") UUID userId);
}
