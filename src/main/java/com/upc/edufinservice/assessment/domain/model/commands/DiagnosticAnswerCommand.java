package com.upc.edufinservice.assessment.domain.model.commands;

import java.util.UUID;

// Este reemplaza al DiagnosticAnswerResource dentro de la capa de aplicación
public record DiagnosticAnswerCommand(
        UUID questionId,
        UUID selectedOptionId,
        Float timeTakenSec
) {}
