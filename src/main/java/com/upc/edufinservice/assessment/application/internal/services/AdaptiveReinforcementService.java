package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.queries.GetQuizQuestionsBySkillQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
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

    /**
     * Selecciona preguntas de refuerzo usando las skills con menor mastery.
     *
     * Versión inicial:
     * - ordena las skills de menor a mayor mastery;
     * - intenta obtener una pregunta QUIZ por skill;
     * - evita repetir questionId dentro del resultado;
     * - continúa recorriendo skills hasta completar reinforcementCount;
     * - no modifica todavía dificultad, módulos desbloqueados ni historial previo.
     *
     * @param mastery mapa skillId -> mastery devuelto por DKT-Forget
     * @param reinforcementCount cantidad máxima de preguntas de refuerzo
     * @return preguntas seleccionadas junto con skill y mastery que motivaron la selección
     */
    public List<ReinforcementQuestionSelection> selectReinforcementQuestions(
            Map<String, Double> mastery,
            int reinforcementCount
    ) {
        if (mastery == null
                || mastery.isEmpty()
                || reinforcementCount <= 0) {
            return List.of();
        }

        /*
         * Pedimos más skills candidatas que preguntas necesarias porque
         * alguna skill podría no tener preguntas QUIZ disponibles.
         */
        List<LowMasterySkill> candidateSkills =
                lowMasterySelector.getLowestMasterySkills(
                        mastery,
                        Math.min(30, Math.max(reinforcementCount * 3, reinforcementCount))
                );

        if (candidateSkills.isEmpty()) {
            return List.of();
        }

        List<ReinforcementQuestionSelection> result = new ArrayList<>();
        Set<UUID> selectedQuestionIds = new HashSet<>();

        for (LowMasterySkill candidate : candidateSkills) {

            if (result.size() >= reinforcementCount) {
                break;
            }

            /*
             * Por ahora pedimos una pregunta aleatoria por skill.
             * Más adelante este punto podrá considerar:
             * - dificultad;
             * - preguntas ya respondidas anteriormente;
             * - módulo actual / módulos desbloqueados;
             * - reglas específicas de QUIZ o FINAL.
             */
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
