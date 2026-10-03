package com.upc.edufinservice.learning.domain.model.queries;

/**
 * Solicita preguntas evaluativas (QUIZ) pertenecientes a una Skill concreta.
 *
 * Se usará posteriormente para construir preguntas de refuerzo a partir
 * de las skills con menor mastery estimado por DKT-Forget.
 */
public record GetQuizQuestionsBySkillQuery(
        Integer skillId,
        int limit
) {
    public GetQuizQuestionsBySkillQuery {
        if (skillId == null || skillId < 1 || skillId > 30) {
            throw new IllegalArgumentException("skillId debe estar entre 1 y 30");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("limit debe ser mayor que 0");
        }
    }
}
