package com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.iam.domain.model.entities.AccountRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRecoveryCodeRepository extends JpaRepository<AccountRecoveryCode, UUID> {
    // Para verificar si el código que ingresó el usuario es el correcto
    Optional<AccountRecoveryCode> findByUserIdAndCode(UUID userId, String code);

    // Para borrar códigos anteriores antes de generarle uno nuevo (evita spam en la BD)
    void deleteByUserId(UUID userId);
}
