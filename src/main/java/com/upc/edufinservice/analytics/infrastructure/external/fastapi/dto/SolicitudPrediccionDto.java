package com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record SolicitudPrediccionDto(

        @JsonProperty("user_id")
        String userId,

        @JsonProperty("interactions")
        List<InteraccionDto> interactions

) {

    public record InteraccionDto(

            @JsonProperty("skill_id")
            Integer skillId,

            @JsonProperty("correct")
            Boolean correct,

            @JsonProperty("timestamp")
            Instant timestamp

    ) {}
}