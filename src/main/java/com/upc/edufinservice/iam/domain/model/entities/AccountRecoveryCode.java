package com.upc.edufinservice.iam.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_recovery_codes")
@Getter
@Setter
@NoArgsConstructor
public class AccountRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación con el usuario que solicitó el cambio
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // El código de 6 dígitos
    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public AccountRecoveryCode(UUID userId, String code) {
        this.userId = userId;
        this.code = code;
        this.createdAt = LocalDateTime.now();
        // El código expira automáticamente en 15 minutos
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    // Método de dominio para saber si el código ya venció
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}