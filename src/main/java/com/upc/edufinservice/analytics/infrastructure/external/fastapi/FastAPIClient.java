package com.upc.edufinservice.analytics.infrastructure.external.fastapi;

import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.ModelStatusDto;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.RespuestaPrediccionDto;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.SolicitudPrediccionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class FastAPIClient {

    private final RestTemplate restTemplate;

    @Value("${ml.engine.url}")
    private String fastapiBaseUrl;

    public FastAPIClient() {
        this.restTemplate = new RestTemplate();
    }

    public ModelStatusDto obtenerEstadoModelo() {
        try {
            return restTemplate.getForObject(
                    fastapiBaseUrl + "/model-status",
                    ModelStatusDto.class
            );
        } catch (Exception e) {
            System.err.println(
                    "[FASTAPI ERROR] No se pudo consultar el estado del modelo: "
                            + e.getMessage()
            );
            return null;
        }
    }

    public RespuestaPrediccionDto obtenerPrediccion(
            SolicitudPrediccionDto solicitud
    ) {
        try {
            return restTemplate.postForObject(
                    fastapiBaseUrl + "/predict",
                    solicitud,
                    RespuestaPrediccionDto.class
            );

        } catch (HttpStatusCodeException e) {

            if (e.getStatusCode().value() == 503) {
                System.out.println(
                        "[FASTAPI] Modelo todavía no disponible. "
                                + "Se continuará sin adaptación."
                );
                return null;
            }

            System.err.println(
                    "[FASTAPI ERROR] HTTP "
                            + e.getStatusCode()
                            + ": "
                            + e.getResponseBodyAsString()
            );

            return null;

        } catch (Exception e) {

            System.err.println(
                    "[FASTAPI ERROR] Fallo en la comunicación con DKT-Forget: "
                            + e.getMessage()
            );

            return null;
        }
    }
}