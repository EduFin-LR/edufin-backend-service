package com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto;

public record RespuestaPrediccionDto(
        String user_id,
        Integer habilidad_objetivo,
        Double probabilidad_base_dkt,
        Double probabilidad_final_dmma,
        String nivel_recommended
) {}