package com.upc.edufinservice.assessment.application.internal.services;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class LowMasterySelector {

    /**
     * Devuelve las skills con menor mastery estimado.
     *
     * Reglas:
     * - ignora entradas nulas;
     * - ignora skill IDs no numéricos;
     * - ignora skills fuera del rango EDUFIN 1..30;
     * - ordena de menor a mayor mastery;
     * - limita la cantidad de resultados.
     *
     * @param mastery mapa recibido desde DKT-Forget: skillId -> probabilidad
     * @param limit cantidad máxima de skills a devolver
     */
    public List<LowMasterySkill> getLowestMasterySkills(
            Map<String, Double> mastery,
            int limit
    ) {
        if (mastery == null || mastery.isEmpty() || limit <= 0) {
            return List.of();
        }

        return mastery.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .filter(entry -> entry.getValue() != null)
                .map(entry -> toLowMasterySkill(entry.getKey(), entry.getValue()))
                .filter(skill -> skill != null)
                .sorted(
                        Comparator
                                .comparingDouble(LowMasterySkill::mastery)
                                .thenComparingInt(LowMasterySkill::skillId)
                )
                .limit(limit)
                .toList();
    }

    private LowMasterySkill toLowMasterySkill(
            String skillIdRaw,
            Double mastery
    ) {
        try {
            int skillId = Integer.parseInt(skillIdRaw);

            if (skillId < 1 || skillId > 30) {
                return null;
            }

            return new LowMasterySkill(
                    skillId,
                    mastery
            );

        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
