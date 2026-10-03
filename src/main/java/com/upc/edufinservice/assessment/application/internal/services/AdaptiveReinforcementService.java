package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.queries.GetQuizQuestionsBySkillQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdaptiveReinforcementService {

    private final LowMasterySelector lowMasterySelector;
    private final LearningQueryService learningQueryService;

    public AdaptiveReinforcementService(
            LowMasterySelector lowMasterySelector,
            LearningQueryService learningQueryService
    ) {
        this.lowMasterySelector = lowMasterySelector;
        this.learningQueryService = learningQueryService;
    }

    public List<ReinforcementQuestionSelection> selectReinforcementQuestions(
            Map<String, Double> mastery,
            Set<Integer> eligibleSkillIds,
            Set<Integer> excludedSkillIds,
            int reinforcementCount
    ) {
        if (mastery == null
                || mastery.isEmpty()
                || eligibleSkillIds == null
                || eligibleSkillIds.isEmpty()
                || reinforcementCount <= 0) {
            return List.of();
        }

        Set<Integer> exclusions =
                excludedSkillIds != null ? excludedSkillIds : Set.of();

        Map<String, Double> filteredMastery = new LinkedHashMap<>();

        mastery.forEach((skillIdRaw, probability) -> {
            if (skillIdRaw == null || probability == null) {
                return;
            }

            try {
                int skillId = Integer.parseInt(skillIdRaw);

                if (eligibleSkillIds.contains(skillId)
                        && !exclusions.contains(skillId)) {
                    filteredMastery.put(
                            String.valueOf(skillId),
                            probability
                    );
                }
            } catch (NumberFormatException ignored) {
            }
        });

        if (filteredMastery.isEmpty()) {
            return List.of();
        }

        List<LowMasterySkill> candidateSkills =
                lowMasterySelector.getLowestMasterySkills(
                        filteredMastery,
                        Math.min(
                                filteredMastery.size(),
                                Math.max(
                                        reinforcementCount * 3,
                                        reinforcementCount
                                )
                        )
                );

        List<ReinforcementQuestionSelection> result = new ArrayList<>();
        Set<UUID> selectedQuestionIds = new HashSet<>();

        for (LowMasterySkill candidate : candidateSkills) {

            if (result.size() >= reinforcementCount) {
                break;
            }

            List<Question> questions = learningQueryService.handle(
                    new GetQuizQuestionsBySkillQuery(
                            candidate.skillId(),
                            1
                    )
            );

            if (questions == null || questions.isEmpty()) {
                continue;
            }

            for (Question question : questions) {

                if (question == null || question.getId() == null) {
                    continue;
                }

                if (!selectedQuestionIds.add(question.getId())) {
                    continue;
                }

                result.add(
                        new ReinforcementQuestionSelection(
                                question,
                                candidate.skillId(),
                                candidate.mastery()
                        )
                );

                break;
            }
        }

        return List.copyOf(result);
    }
}
