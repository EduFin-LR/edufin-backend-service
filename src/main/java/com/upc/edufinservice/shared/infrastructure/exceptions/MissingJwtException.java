package com.upc.edufinservice.shared.infrastructure.exceptions;

public class MissingJwtException extends RuntimeException {

    public MissingJwtException() {
        super("No se detectó un JWT válido en la solicitud.");
    }
}