package com.upc.edufinservice.iam.interfaces.rest;

import com.upc.edufinservice.iam.application.internal.commandservices.EmailVerificationCommandServiceImpl;
import com.upc.edufinservice.iam.interfaces.rest.resources.ResendVerificationResource;
import com.upc.edufinservice.iam.interfaces.rest.resources.VerifyEmailResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iam/auth")
public class EmailVerificationController {

    private final EmailVerificationCommandServiceImpl emailVerificationService;

    public EmailVerificationController(
            EmailVerificationCommandServiceImpl emailVerificationService
    ) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @RequestBody VerifyEmailResource resource
    ) {

        emailVerificationService.verifyEmail(
                resource.email(),
                resource.code()
        );

        return ResponseEntity.ok(
                "Correo verificado correctamente"
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(
            @RequestBody ResendVerificationResource resource
    ) {

        emailVerificationService.sendVerificationCode(
                resource.email()
        );

        return ResponseEntity.ok(
                "Código de verificación reenviado"
        );
    }
}