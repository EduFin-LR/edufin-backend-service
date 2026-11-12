package com.upc.edufinservice.iam.application.internal.commandservices;

import com.upc.edufinservice.iam.domain.model.entities.AccountRecoveryCode;
import com.upc.edufinservice.iam.domain.model.entities.VerificationCodePurpose;
import com.upc.edufinservice.iam.infrastructure.email.EmailService;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.AccountRecoveryCodeRepository;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailVerificationCommandServiceImpl {

    private final UserRepository userRepository;
    private final AccountRecoveryCodeRepository recoveryCodeRepository;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationCommandServiceImpl(
            UserRepository userRepository,
            AccountRecoveryCodeRepository recoveryCodeRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationCode(String email) {

        var user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuario no encontrado")
                );

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya fue verificado. Inicia sesión.");
        }

        recoveryCodeRepository.deleteByUserIdAndPurpose(
                user.getId(),
                VerificationCodePurpose.EMAIL_VERIFICATION
        );

        String code = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        var verificationCode = new AccountRecoveryCode(
                user.getId(),
                code,
                VerificationCodePurpose.EMAIL_VERIFICATION
        );

        recoveryCodeRepository.save(verificationCode);

        emailService.sendVerificationCode(
                user.getEmail(),
                user.getUsername(),
                code
        );
    }

    @Transactional
    public void verifyEmail(String email, String code) {

        var user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuario no encontrado")
                );

        // Una cuenta ya verificada no debe aceptar códigos arbitrarios con HTTP 200.
        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya fue verificado. Inicia sesión.");
        }

        if (code == null || !code.matches("\\d{6}")) {
            throw new IllegalArgumentException("El código de verificación debe tener 6 dígitos");
        }

        var verificationCode =
                recoveryCodeRepository.findByUserIdAndCodeAndPurpose(
                        user.getId(),
                        code,
                        VerificationCodePurpose.EMAIL_VERIFICATION
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Código de verificación inválido"
                        )
                );

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {

            recoveryCodeRepository.deleteByUserIdAndPurpose(
                    user.getId(),
                    VerificationCodePurpose.EMAIL_VERIFICATION
            );

            throw new IllegalArgumentException(
                    "El código de verificación ha expirado"
            );
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        recoveryCodeRepository.deleteByUserIdAndPurpose(
                user.getId(),
                VerificationCodePurpose.EMAIL_VERIFICATION
        );
    }
}