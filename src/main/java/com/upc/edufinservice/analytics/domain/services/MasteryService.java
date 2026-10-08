package com.upc.edufinservice.analytics.domain.services;

import com.upc.edufinservice.analytics.domain.model.entities.MasterySnapshotSource;

import java.util.Map;
import java.util.UUID;

/**
 * Contrato para almacenar y consultar el último snapshot de mastery
 * devuelto por DKT-Forget para un usuario.
 */
public interface MasteryService {

    void updateMasterySnapshot(
            UUID userId,
            Map<String, Double> mastery
    );

    Map<String, Double> getMasterySnapshot(UUID userId);

    boolean hasMasterySnapshot(UUID userId);

    /**
     * Persiste el estado actual de las 30 skills como un punto histórico.
     * Se invoca solo en hitos relevantes (PRE_TEST, QUIZ y FINAL).
     */
    void recordHistorySnapshot(
            UUID userId,
            MasterySnapshotSource source,
            UUID contextId
    );
}
