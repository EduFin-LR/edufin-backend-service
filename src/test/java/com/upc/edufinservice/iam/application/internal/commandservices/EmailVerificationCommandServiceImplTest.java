package com.upc.edufinservice.iam.application.internal.commandservices;

import com.upc.edufinservice.iam.domain.model.aggregates.User;
import com.upc.edufinservice.iam.domain.model.entities.AccountRecoveryCode;
import com.upc.edufinservice.iam.domain.model.entities.VerificationCodePurpose;
import com.upc.edufinservice.iam.infrastructure.email.EmailService;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.AccountRecoveryCodeRepository;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCommandServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRecoveryCodeRepository recoveryCodeRepository;
    @Mock private EmailService emailService;
    @InjectMocks private EmailVerificationCommandServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("estudiante", "estudiante@example.test", "hash", "MALE");
        user.setId(UUID.randomUUID());
    }

    @Test
    void rejectsAnyCodeForAnAlreadyVerifiedAccount() {
        user.setEmailVerified(true);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.verifyEmail(user.getEmail(), "123456"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(recoveryCodeRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsAnIncorrectCodeForPendingAccount() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(recoveryCodeRepository.findByUserIdAndCodeAndPurpose(
                user.getId(), "654321", VerificationCodePurpose.EMAIL_VERIFICATION
        )).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.verifyEmail(user.getEmail(), "654321"));

        assertEquals("Código de verificación inválido", error.getMessage());
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void acceptsAValidCodeForPendingAccount() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        AccountRecoveryCode activeCode = new AccountRecoveryCode(
                user.getId(), "123456", VerificationCodePurpose.EMAIL_VERIFICATION
        );
        when(recoveryCodeRepository.findByUserIdAndCodeAndPurpose(
                user.getId(), "123456", VerificationCodePurpose.EMAIL_VERIFICATION
        )).thenReturn(Optional.of(activeCode));

        service.verifyEmail(user.getEmail(), "123456");

        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
        verify(recoveryCodeRepository).deleteByUserIdAndPurpose(
                user.getId(), VerificationCodePurpose.EMAIL_VERIFICATION);
    }

    @Test
    void rejectsResendingToAlreadyVerifiedAccount() {
        user.setEmailVerified(true);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.sendVerificationCode(user.getEmail()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(recoveryCodeRepository, emailService);
    }
}
