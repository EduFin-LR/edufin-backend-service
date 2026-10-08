package com.upc.edufinservice.analytics.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.MasterySnapshotSource;
import com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryHistory;
import com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryPrediction;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.SkillMasteryHistoryRepository;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.SkillMasteryPredictionRepository;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MasteryServiceImpl implements MasteryService {

    private final SkillMasteryPredictionRepository repository;
    private final SkillMasteryHistoryRepository historyRepository;
    private final StudentInteractionRepository interactionRepository;

    @Value("${ml.engine.model-version:dkt_forget_pretrained_v1}")
    private String modelVersion;

    public MasteryServiceImpl(
            SkillMasteryPredictionRepository repository,
            SkillMasteryHistoryRepository historyRepository,
            StudentInteractionRepository interactionRepository
    ) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.interactionRepository = interactionRepository;
    }

    @Override
    @Transactional
    public void updateMasterySnapshot(
            UUID userId,
            Map<String, Double> mastery
    ) {
        if (userId == null || mastery == null || mastery.isEmpty()) {
            return;
        }

        mastery.forEach((skillIdRaw, masteryValue) -> {
            if (skillIdRaw == null || masteryValue == null) {
                return;
            }

            final int skillId;
            try {
                skillId = Integer.parseInt(skillIdRaw);
            } catch (NumberFormatException ex) {
                return;
            }

            if (skillId < 1 || skillId > 30) {
                return;
            }

            if (masteryValue < 0.0 || masteryValue > 1.0) {
                return;
            }

            var prediction = repository
                    .findByUserIdAndSkillId(userId, skillId)
                    .orElseGet(() ->
                            new SkillMasteryPrediction(
                                    userId,
                                    skillId,
                                    masteryValue
                            )
                    );

            prediction.updateMastery(masteryValue);
            repository.save(prediction);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getMasterySnapshot(UUID userId) {
        Map<String, Double> result = new LinkedHashMap<>();

        if (userId == null) {
            return result;
        }

        repository.findByUserId(userId)
                .stream()
                .sorted((a, b) ->
                        Integer.compare(a.getSkillId(), b.getSkillId())
                )
                .forEach(prediction ->
                        result.put(
                                String.valueOf(prediction.getSkillId()),
                                prediction.getMastery()
                        )
                );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMasterySnapshot(UUID userId) {
        return userId != null
                && !repository.findByUserId(userId).isEmpty();
    }

    @Override
    @Transactional
    public void recordHistorySnapshot(
            UUID userId,
            MasterySnapshotSource source,
            UUID contextId
    ) {
        if (userId == null || source == null) {
            return;
        }

        var currentSnapshot = repository.findByUserId(userId);

        if (currentSnapshot.isEmpty()) {
            return;
        }

        Instant recordedAt = Instant.now();
        long interactionCount = interactionRepository.countByUserId(userId);
        String effectiveModelVersion =
                modelVersion == null || modelVersion.isBlank()
                        ? "unknown"
                        : modelVersion.trim();

        var historyRows = new ArrayList<SkillMasteryHistory>();

        currentSnapshot.stream()
                .sorted((a, b) -> Integer.compare(a.getSkillId(), b.getSkillId()))
                .forEach(prediction -> historyRows.add(
                        new SkillMasteryHistory(
                                userId,
                                prediction.getSkillId(),
                                prediction.getMastery(),
                                recordedAt,
                                interactionCount,
                                effectiveModelVersion,
                                source,
                                contextId
                        )
                ));

        historyRepository.saveAll(historyRows);
    }
}
