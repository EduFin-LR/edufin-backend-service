package com.upc.edufinservice.iam.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.upc.edufinservice.iam.application.internal.commandservices.PasswordRecoveryCommandServiceImpl;
import com.upc.edufinservice.iam.interfaces.rest.resources.ForgotPasswordResource;
import com.upc.edufinservice.iam.interfaces.rest.resources.MessageResource;
import com.upc.edufinservice.iam.interfaces.rest.resources.ResetPasswordResource;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/iam/auth")
@Tag(name = "Authentication", description = "Endpoints de seguridad y recuperación de cuentas")
public class PasswordRecoveryController {

    private final PasswordRecoveryCommandServiceImpl passwordRecoveryService;

    public PasswordRecoveryController(PasswordRecoveryCommandServiceImpl passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResource> forgotPassword(@RequestBody ForgotPasswordResource resource) {
        // Ejecutamos el envío de correo
        passwordRecoveryService.generateAndSendRecoveryCode(resource.email());

        // Siempre devolvemos OK por seguridad (evita ataques de enumeración de correos)
        return ResponseEntity.ok(new MessageResource("Si el correo existe, se ha enviado un código de recuperación."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResource> resetPassword(@RequestBody ResetPasswordResource resource) {
        boolean success = passwordRecoveryService.resetPassword(
                resource.email(),
                resource.code(),
                resource.newPassword()
        );

        if (success) {
            return ResponseEntity.ok(new MessageResource("Contraseña actualizada correctamente."));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código es incorrecto o ha expirado.");
    }
}