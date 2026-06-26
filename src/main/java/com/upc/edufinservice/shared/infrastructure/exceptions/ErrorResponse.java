package com.upc.edufinservice.shared.infrastructure.exceptions;

import java.time.LocalDateTime;

// Este record será el JSON que reciba tu frontend
public record ErrorResponse(
        int statusCode,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public ErrorResponse(int statusCode, String error, String message) {
        this(statusCode, error, message, LocalDateTime.now());
    }
}