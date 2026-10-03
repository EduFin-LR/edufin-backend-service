package com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelStatusDto(

        @JsonProperty("model_ready")
        Boolean modelReady,

        @JsonProperty("model_name")
        String modelName,

        @JsonProperty("num_skills")
        Integer numSkills,

        @JsonProperty("detail")
        String detail

) {}