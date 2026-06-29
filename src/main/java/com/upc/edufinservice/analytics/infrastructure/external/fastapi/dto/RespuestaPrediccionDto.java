package com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RespuestaPrediccionDto(
        @JsonProperty("user_id") String user_id,
        @JsonProperty("habilidad_objetivo") Integer habilidad_objetivo,
        @JsonProperty("probabilidad_base_dkt") Double probabilidad_base_dkt,
        @JsonProperty("probabilidad_final_dmma") Double probabilidad_final_dmma,
        @JsonProperty("nivel_recomendado") String nivel_recomendado
) {}