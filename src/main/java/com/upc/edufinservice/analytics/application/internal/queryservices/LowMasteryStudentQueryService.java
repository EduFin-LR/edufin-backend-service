package com.upc.edufinservice.analytics.application.internal.queryservices;

import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.SkillMasteryHistoryRepository;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.upc.edufinservice.analytics.interfaces.rest.resources.LowMasteryStudentResource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LowMasteryStudentQueryService {

    private static final double LOW_MASTERY_THRESHOLD = 0.60;

    private final SkillMasteryHistoryRepository masteryRepository;
    private final UserRepository userRepository;

    public LowMasteryStudentQueryService(
            SkillMasteryHistoryRepository masteryRepository,
            UserRepository userRepository
    ) {
        this.masteryRepository = masteryRepository;
        this.userRepository = userRepository;
    }

    public List<LowMasteryStudentResource> getLowMasteryStudents() {

        var latestMasteries = masteryRepository.findLatestMasteryByUserAndSkill();

        Map<UUID, List<com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryHistory>>
                groupedByUser =
                latestMasteries.stream()
                        .collect(Collectors.groupingBy(
                                com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryHistory::getUserId
                        ));

        return groupedByUser.entrySet()
                .stream()
                .map(entry -> {

                    UUID userId = entry.getKey();
                    var masteries = entry.getValue();

                    var user = userRepository.findById(userId)
                            .orElse(null);

                    if (user == null) {
                        return null;
                    }

                    double averageMastery =
                            masteries.stream()
                                    .mapToDouble(
                                            com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryHistory::getMastery
                                    )
                                    .average()
                                    .orElse(0.0);

                    long lowMasterySkills =
                            masteries.stream()
                                    .filter(history ->
                                            history.getMastery() < LOW_MASTERY_THRESHOLD
                                    )
                                    .count();

                    return new LowMasteryStudentResource(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            averageMastery,
                            lowMasterySkills
                    );
                })
                .filter(resource -> resource != null)
                .filter(resource -> resource.lowMasterySkills() > 0)
                .sorted(
                        Comparator.comparingLong(
                                LowMasteryStudentResource::lowMasterySkills
                        ).reversed()
                )
                .toList();
    }
}