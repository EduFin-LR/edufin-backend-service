package com.upc.edufinservice.iam.domain.model.queries;

public record GetUserByIdQuery(Long userId) {
    public GetUserByIdQuery{
        if(userId == null || userId <= 0){
            throw new IllegalArgumentException("El ID del usuario debe ser mayor a cero");
        }
    }
}
