package com.upc.edufinservice.analytics.infrastructure.external.fastapi;

import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.SolicitudPrediccionDto;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.RespuestaPrediccionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FastAPIClient {

    private final RestTemplate restTemplate;
    // Ajustado a la ruta exacta de tu archivo de Python

    @Value("${ml.engine.url}")
    private String fastapiUrl;

    public FastAPIClient() {
        this.restTemplate = new RestTemplate();
    }

    public RespuestaPrediccionDto obtenerPrediccion(SolicitudPrediccionDto solicitud) {
        try {
            // Realiza la llamada POST enviando el contrato requerido y esperando la respuesta mapeada
            return restTemplate.postForObject(fastapiUrl, solicitud, RespuestaPrediccionDto.class);
        } catch (Exception e) {
            System.err.println("[FASTAPI ERROR] Fallo en la comunicación con la IA: " + e.getMessage());
            return null;
        }
    }
}