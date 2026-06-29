package com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SolicitudPrediccionDto(
        @JsonProperty("user_id") String user_id,
        @JsonProperty("secuencia_interacciones") List<Integer> secuencia_interacciones,
        @JsonProperty("habilidad_objetivo") Integer habilidad_objetivo,
        @JsonProperty("dias_inactividad") Double dias_inactividad
) {}