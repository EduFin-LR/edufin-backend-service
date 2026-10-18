package com.upc.edufinservice.iam.infrastructure.email;

public interface EmailService {

    void sendRecoveryCode(String recipientEmail, String recipientName, String recoveryCode);

    void sendVerificationCode(String toEmail, String username, String code);
}
