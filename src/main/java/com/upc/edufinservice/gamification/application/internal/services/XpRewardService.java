package com.upc.edufinservice.gamification.application.internal.services;

import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.XpRewardGrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class XpRewardService {
    public static final String QUESTION_CORRECT = "QUESTION_CORRECT";
    public static final String LESSON_COMPLETED = "LESSON_COMPLETED";

    private final XpRewardGrantRepository rewardRepository;
    private final GamificationCommandService gamificationCommandService;

    public XpRewardService(XpRewardGrantRepository rewardRepository,
                           GamificationCommandService gamificationCommandService) {
        this.rewardRepository = rewardRepository;
        this.gamificationCommandService = gamificationCommandService;
    }

    @Transactional
    public boolean awardOnce(UUID userId, UUID sourceId, String rewardType, int points) {
        if (points <= 0) {
            return false;
        }
        int inserted = rewardRepository.insertIfAbsent(
                UUID.randomUUID(), userId, sourceId, rewardType, points
        );
        if (inserted == 0) {
            return false;
        }
        gamificationCommandService.handle(new AddPointsCommand(userId, points));
        return true;
    }
}
