package com.upc.edufinservice.iam.domain.model.queries;

import java.util.UUID;

public record GetUserByIdQuery(UUID userId) {
    public GetUserByIdQuery{
        if(userId == null){
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }
    }
}
