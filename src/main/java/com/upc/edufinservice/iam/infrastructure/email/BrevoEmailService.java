package com.upc.edufinservice.iam.infrastructure.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService implements EmailService {

    private final WebClient webClient;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.from-email}")
    private String fromEmail;

    @Value("${brevo.from-name}")
    private String fromName;

    public BrevoEmailService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    @Override
    public void sendRecoveryCode(String toEmail, String username, String code) {

        Map<String, Object> sender = Map.of(
                "name", fromName,
                "email", fromEmail
        );

        Map<String, Object> recipient = Map.of(
                "email", toEmail
        );

        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", List.of(recipient),
                "subject", "Código de recuperación - EduFin",
                "htmlContent", """
                    <div style="font-family: Arial, sans-serif;">
                        <h2>Recuperación de contraseña</h2>

                        <p>Hola %s,</p>

                        <p>Tu código de recuperación de EduFin es:</p>

                        <h1 style="letter-spacing: 6px;">
                            %s
                        </h1>

                        <p>Este código tiene un tiempo limitado de validez.</p>

                        <p>Si no solicitaste este cambio, puedes ignorar este correo.</p>

                        <br>

                        <p>EduFin</p>
                    </div>
                    """.formatted(username, code)
        );

        try {

            webClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException e) {

            throw new IllegalStateException(
                    "Brevo rechazó el envío del correo (HTTP "
                            + e.getStatusCode().value()
                            + "): "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    @Override
    public void sendVerificationCode(
            String toEmail,
            String username,
            String code
    ) {

        Map<String, Object> sender = Map.of(
                "name", fromName,
                "email", fromEmail
        );

        Map<String, Object> recipient = Map.of(
                "email", toEmail
        );

        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", List.of(recipient),
                "subject", "Verifica tu correo - EduFin",
                "htmlContent", """
                    <div style="font-family: Arial, sans-serif;">
                        <h2>Verificación de correo</h2>

                        <p>Hola %s,</p>

                        <p>Gracias por registrarte en EduFin.</p>

                        <p>Tu código de verificación es:</p>

                        <h1 style="letter-spacing: 6px;">
                            %s
                        </h1>

                        <p>Este código expirará en 15 minutos.</p>

                        <p>Si no creaste esta cuenta, puedes ignorar este correo.</p>

                        <br>

                        <p>EduFin</p>
                    </div>
                    """.formatted(username, code)
        );

        try {

            webClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException e) {

            throw new IllegalStateException(
                    "Brevo rechazó el envío del correo de verificación (HTTP "
                            + e.getStatusCode().value()
                            + "): "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }
}