package com.upc.edufinservice.assessment.application.internal.queryservices;

import com.upc.edufinservice.assessment.domain.services.AssessmentQueryService;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AssessmentQueryServiceImpl implements AssessmentQueryService {

    private final UserLessonProgressRepository userLessonProgressRepository;

    public AssessmentQueryServiceImpl(UserLessonProgressRepository userLessonProgressRepository) {
        this.userLessonProgressRepository = userLessonProgressRepository;
    }

    @Override
    public int getCompletedLessonsCount(UUID userId, List<UUID> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return 0;
        return userLessonProgressRepository.countByUserIdAndLessonIdInAndStatus(userId, lessonIds, ProgressStatus.COMPLETED);
    }

    @Override
    public String getLessonStatus(UUID userId, UUID lessonId, boolean isFirstLessonOfApp) {
        return userLessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .map(progress -> progress.getStatus().name())
                .orElse(isFirstLessonOfApp ? "UNLOCKED" : "LOCKED");
    }
}