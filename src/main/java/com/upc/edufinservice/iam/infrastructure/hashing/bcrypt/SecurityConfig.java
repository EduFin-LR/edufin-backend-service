package com.upc.edufinservice.iam.infrastructure.hashing.bcrypt;

import com.upc.edufinservice.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import com.upc.edufinservice.iam.infrastructure.tokens.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
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
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
                // Desactivamos CSRF porque usaremos JWT (no necesitamos protección de formularios)
                .csrf(AbstractHttpConfigurer::disable)

                // Configuramos la política de sesiones como "sin estado" (stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Definimos las reglas de acceso a las rutas
                .authorizeHttpRequests(auth -> auth
                        // Permitimos libre acceso a todo lo que esté bajo /iam/auth (login, registro, etc)
                        .requestMatchers("/iam/auth/**").permitAll()
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

        // 1. Dominios permitidos (Aquí pones el puerto donde correrá tu React)
        // Puedes agregar más si luego lo subes a la nube (ej. "https://mi-tesis.com")
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));

        // 2. Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. Cabeceras permitidas (Crucial para que pase el Authorization: Bearer)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // 4. Aplicar a todas las rutas de tu API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
