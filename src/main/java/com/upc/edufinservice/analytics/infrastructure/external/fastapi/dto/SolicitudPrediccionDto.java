package com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto;

import java.util.List;

public record SolicitudPrediccionDto(
        String user_id,
        List<Integer> secuencia_interacciones,
        Integer habilidad_objetivo,
        Double dias_inactividad
) {}