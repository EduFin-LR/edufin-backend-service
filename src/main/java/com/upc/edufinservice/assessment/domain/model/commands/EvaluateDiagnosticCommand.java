package com.upc.edufinservice.assessment.domain.model.commands;

import java.util.List;
import java.util.UUID;

// Este es el paquete seguro que el Controlador le entregará a tu Servicio
public record EvaluateDiagnosticCommand(
        UUID userId,
        List<DiagnosticAnswerCommand> answers
) {}
