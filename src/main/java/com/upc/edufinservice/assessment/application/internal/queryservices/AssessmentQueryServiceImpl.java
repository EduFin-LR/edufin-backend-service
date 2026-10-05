package com.upc.edufinservice.assessment.application.internal.queryservices;

import com.upc.edufinservice.assessment.domain.services.AssessmentQueryService;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentPhase;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalAssessmentSessionRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.TopicFinalResultRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AssessmentQueryServiceImpl implements AssessmentQueryService {

    private final UserLessonProgressRepository _userLessonProgressRepository;
    private final ExperimentalAssessmentSessionRepository _experimentalAssessmentSessionRepository;
    private final TopicFinalResultRepository _topicFinalResultRepository;

    public AssessmentQueryServiceImpl(
            UserLessonProgressRepository userLessonProgressRepository,
            ExperimentalAssessmentSessionRepository experimentalAssessmentSessionRepository,
            TopicFinalResultRepository topicFinalResultRepository
    ) {
        _userLessonProgressRepository = userLessonProgressRepository;
        _experimentalAssessmentSessionRepository = experimentalAssessmentSessionRepository;
        _topicFinalResultRepository = topicFinalResultRepository;
    }

    @Override
    public int getCompletedLessonsCount(UUID userId, List<UUID> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return 0;
        return _userLessonProgressRepository.countByUserIdAndLessonIdInAndStatus(userId, lessonIds, ProgressStatus.COMPLETED);
    }

    @Override
    public String getLessonStatus(UUID userId, UUID lessonId, boolean isFirstLessonOfApp) {
        return _userLessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .map(progress -> progress.getStatus().name())
                .orElse(isFirstLessonOfApp ? "UNLOCKED" : "LOCKED");
    }

    @Override
    public boolean hasCompletedDiagnostic(UUID userId) {
        // El "diagnostic_test" del perfil ahora representa haber completado el PRE_TEST experimental.
        return _experimentalAssessmentSessionRepository
                .existsByUserIdAndPhase(userId, ExperimentalAssessmentPhase.PRE_TEST);
    }

    @Override
    public boolean hasPassedTopicFinal(
            UUID userId,
            UUID topicId
    ) {
        return _topicFinalResultRepository
                .existsByUserIdAndTopicIdAndPassedTrue(
                        userId,
                        topicId
                );
    }
}
