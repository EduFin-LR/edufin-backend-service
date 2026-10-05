package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.StudentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudentInteractionRepository extends JpaRepository<StudentInteraction, UUID> {

    List<StudentInteraction> findByUserIdOrderByInteractedAtAsc(UUID userId);

    /**
     * Skills realmente trabajadas dentro del recorrido formativo.
     * PRE_TEST puede existir en StudentInteraction para inicializar DKT, pero los
     * consumidores de esta consulta deciden explícitamente qué tipos habilitan refuerzo.
     */
    @Query("""
            SELECT DISTINCT si.dktSkillId
            FROM StudentInteraction si
            WHERE si.userId = :userId
              AND si.interactionType IN :interactionTypes
            """)
    List<Integer> findDistinctSkillIdsByUserIdAndInteractionTypeIn(
            @Param("userId") UUID userId,
            @Param("interactionTypes") Collection<InteractionType> interactionTypes
    );
}
