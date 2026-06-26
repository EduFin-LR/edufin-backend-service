package com.upc.edufinservice.shared.infrastructure.exceptions;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Manejar credenciales inválidas (El caso que mencionaste)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Credenciales inválidas. Verifica tu correo y contraseña."
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 2. Manejar argumentos inválidos (Ej. si mandan un email vacío)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

        @ExceptionHandler(MissingJwtException.class)
        public ResponseEntity<ErrorResponse> handleMissingJwt(MissingJwtException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                ex.getMessage()
                );
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(InvalidJwtException.class)
        public ResponseEntity<ErrorResponse> handleInvalidJwt(InvalidJwtException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                ex.getMessage()
                );
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
                HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
                ErrorResponse error = new ErrorResponse(
                                status.value(),
                                status.getReasonPhrase(),
                                ex.getReason() != null ? ex.getReason() : "Ocurrió un error inesperado en el servidor."
                );
                return new ResponseEntity<>(error, status);
        }

    // 3. Comodín: Atrapa CUALQUIER otro error que no hayamos previsto (Error 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocurrió un error inesperado en el servidor."
        );
        logger.error("Unhandled server error", ex);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Extraemos todos los errores de los campos y los unimos en un solo texto
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Datos inválidos: " + errorMessage
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "No tienes los permisos necesarios para realizar esta acción."
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

        @ExceptionHandler({
                NoHandlerFoundException.class,
                NoResourceFoundException.class
        })
        public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    "El recurso solicitado no existe."
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler({
                HttpRequestMethodNotSupportedException.class
        })
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse error = new ErrorResponse(
                    HttpStatus.METHOD_NOT_ALLOWED.value(),
                    "Method Not Allowed",
                    "El método HTTP utilizado no está permitido para este endpoint."
        );
        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
        }

        @ExceptionHandler({
                HttpMediaTypeNotSupportedException.class,
                HttpMediaTypeNotAcceptableException.class
        })
        public ResponseEntity<ErrorResponse> handleMediaTypeProblems(Exception ex) {
        HttpStatus status = ex instanceof HttpMediaTypeNotSupportedException
                    ? HttpStatus.UNSUPPORTED_MEDIA_TYPE
                    : HttpStatus.NOT_ACCEPTABLE;

        ErrorResponse error = new ErrorResponse(
                    status.value(),
                    status.getReasonPhrase(),
                    status == HttpStatus.UNSUPPORTED_MEDIA_TYPE
                            ? "El tipo de contenido enviado no es compatible."
                            : "La respuesta solicitada no puede generarse en el formato pedido."
        );
        return new ResponseEntity<>(error, status);
        }

        @ExceptionHandler({
                MissingServletRequestParameterException.class,
                MethodArgumentTypeMismatchException.class
        })
        public ResponseEntity<ErrorResponse> handleBadRequestParameters(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    "Uno o más parámetros de la solicitud son inválidos."
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                "El JSON enviado no es válido o no tiene el formato esperado."
                );
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
}