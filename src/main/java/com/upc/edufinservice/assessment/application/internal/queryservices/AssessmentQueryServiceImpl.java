package com.upc.edufinservice.assessment.application.internal.queryservices;

import com.upc.edufinservice.assessment.domain.services.AssessmentQueryService;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.DiagnosticResultRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AssessmentQueryServiceImpl implements AssessmentQueryService {

    private final UserLessonProgressRepository _userLessonProgressRepository;
    private final DiagnosticResultRepository _diagnosticResultRepository;

    public AssessmentQueryServiceImpl(
            UserLessonProgressRepository userLessonProgressRepository,
            DiagnosticResultRepository diagnosticResultRepository
    ) {
        _userLessonProgressRepository = userLessonProgressRepository;
        _diagnosticResultRepository = diagnosticResultRepository;
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
        // Retorna true si ya existe una fila de resultados para el estudiante, false si no
        return _diagnosticResultRepository.findByUserId(userId).isPresent();
    }
}