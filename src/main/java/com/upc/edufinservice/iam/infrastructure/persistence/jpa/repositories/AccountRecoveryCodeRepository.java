package com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.iam.domain.model.entities.AccountRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.upc.edufinservice.iam.domain.model.entities.VerificationCodePurpose;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRecoveryCodeRepository extends JpaRepository<AccountRecoveryCode, UUID> {
    Optional<AccountRecoveryCode> findByUserIdAndCodeAndPurpose(
            UUID userId,
            String code,
            VerificationCodePurpose purpose
    );

    void deleteByUserIdAndPurpose(
            UUID userId,
            VerificationCodePurpose purpose
    );
}
