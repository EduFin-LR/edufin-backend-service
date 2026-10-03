package com.upc.edufinservice.analytics.domain.services;

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
}
