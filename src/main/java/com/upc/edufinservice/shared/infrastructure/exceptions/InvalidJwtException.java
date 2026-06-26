package com.upc.edufinservice.shared.infrastructure.exceptions;

public class InvalidJwtException extends RuntimeException {

    public InvalidJwtException() {
        super("El JWT enviado es inválido o expiró.");
    }
}