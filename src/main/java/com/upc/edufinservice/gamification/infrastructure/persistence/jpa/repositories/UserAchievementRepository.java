package com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.gamification.domain.model.aggregates.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {
    // Para ver qué medallas ya tiene el usuario y no dárselas dos veces
    List<UserAchievement> findByUserId(UUID userId);
    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);
}