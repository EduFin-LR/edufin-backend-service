package com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record RespuestaPrediccionDto(

        @JsonProperty("user_id")
        String userId,

        @JsonProperty("model_ready")
        Boolean modelReady,

        @JsonProperty("mastery")
        Map<String, Double> mastery

) {}