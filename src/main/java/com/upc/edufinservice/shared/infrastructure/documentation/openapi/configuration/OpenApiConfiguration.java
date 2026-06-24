package com.upc.edufinservice.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI edufinanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduFin API")
                        .description("API REST para la Aplicación Web adaptativa de Aprendizaje Gamificado mediante Deep Knowledge Tracing para la Educación Financiera.")
                        .version("v1.0.0"));
    }
}
