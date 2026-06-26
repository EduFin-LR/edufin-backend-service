package com.upc.edufinservice.shared.infrastructure.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

// Este record será el JSON que reciba tu frontend
public record ErrorResponse(
        int statusCode,
        String error,
        String message,

        // 🚀 Le decimos a Jackson exactamente cómo formatear esta fecha
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public ErrorResponse(int statusCode, String error, String message) {
        this(statusCode, error, message, LocalDateTime.now());
    }
}