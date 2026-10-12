package com.upc.edufinservice.iam.infrastructure.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService implements EmailService {

    private static final String RESEND_BASE_URL = "https://api.resend.com";

    private final WebClient webClient;
    private final String apiKey;
    private final String from;

    public ResendEmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:EduFin <onboarding@resend.dev>}") String from) {
        this.apiKey = apiKey;
        this.from = from;
        this.webClient = WebClient.builder()
                .baseUrl(RESEND_BASE_URL)
                .build();
    }

    @Override
    public void sendRecoveryCode(String recipientEmail, String recipientName, String recoveryCode) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY no está configurada.");
        }

        String text = "Hola " + recipientName + ",\n\n" +
                "Tu código para restablecer tu contraseña es: " + recoveryCode + "\n\n" +
                "Este código expirará en 15 minutos.\n" +
                "Si no solicitaste este cambio, ignora este correo de forma segura.";

        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(recipientEmail),
                "subject", "Código de Recuperación de Contraseña - EduFin",
                "text", text
        );

        try {
            webClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException(
                    "Resend rechazó el envío del correo (HTTP " + ex.getStatusCode().value() + "): " +
                            ex.getResponseBodyAsString(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo enviar el correo de recuperación mediante Resend.", ex);
        }
    }
}
