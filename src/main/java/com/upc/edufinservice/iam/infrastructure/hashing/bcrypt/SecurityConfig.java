package com.upc.edufinservice.iam.infrastructure.hashing.bcrypt;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.edufinservice.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import com.upc.edufinservice.iam.infrastructure.tokens.jwt.JwtTokenProvider;
import com.upc.edufinservice.shared.infrastructure.exceptions.ErrorResponse;
import com.upc.edufinservice.shared.infrastructure.exceptions.InvalidJwtException;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider _tokenProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider){
        _tokenProvider = tokenProvider;
    }

    // 1. Aquí exponemos el encriptador para poder inyectarlo en tus servicios
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

// 2. Desactivamos el bloqueo automático para que tu Swagger y endpoints sigan libres
    // (Más adelante, cuando configures los JWT, ajustaremos esto)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(exception -> exception
                        // Maneja el 401 (No hay token o es inválido)
                        .authenticationEntryPoint((request, response, authException) -> {
                        Object invalidJwt = request.getAttribute(JwtAuthenticationFilter.INVALID_JWT_ATTRIBUTE);
                        String message = invalidJwt instanceof InvalidJwtException
                            ? ((InvalidJwtException) invalidJwt).getMessage()
                            : new MissingJwtException().getMessage();

                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            ErrorResponse errorResponse = new ErrorResponse(
                                    401,
                                    "Unauthorized",
                            message
                            );
                            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
                            response.getWriter().flush();
                        })
                        // Maneja el 403 (Hay token válido, pero intentó entrar a una ruta prohibida)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            ErrorResponse errorResponse = new ErrorResponse(
                                    403,
                                    "Forbidden",
                                    "Tu rol actual no tiene acceso a este recurso."
                            );
                            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
                            response.getWriter().flush();
                        })
                )
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
                // Desactivamos CSRF porque usaremos JWT (no necesitamos protección de formularios)
                .csrf(AbstractHttpConfigurer::disable)

                // Configuramos la política de sesiones como "sin estado" (stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Definimos las reglas de acceso a las rutas
                .authorizeHttpRequests(auth -> auth
                        // Permitimos libre acceso a todo lo que esté bajo /iam/auth (login, registro, etc)
                        .requestMatchers("/iam/auth/**").permitAll()
                    // Permitimos el endpoint interno de error para evitar respuestas HTML por defecto
                    .requestMatchers("/error").permitAll()
                        // Permitimos libre acceso a Swagger para poder probar
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // TODO: Aquí pondremos las rutas protegidas en el futuro
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(_tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));
        // 1. Dominios permitidos (Aquí pones el puerto donde correrá tu React)
        // Puedes agregar más si luego lo subes a la nube (ej. "https://mi-tesis.com")
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));

        // 2. Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. Cabeceras permitidas (Crucial para que pase el Authorization: Bearer)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        configuration.setAllowCredentials(true);

        // 4. Aplicar a todas las rutas de tu API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
