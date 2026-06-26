package com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.gamification.domain.model.aggregates.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {


}