package com.upc.edufinservice.iam.infrastructure.tokens.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

import com.upc.edufinservice.shared.infrastructure.exceptions.InvalidJwtException;
import io.jsonwebtoken.JwtException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INVALID_JWT_ATTRIBUTE = "edufin.invalid-jwt";

    private final JwtTokenProvider _tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        _tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraemos el token del request usando nuestro método helper
        String token = getJwtFromRequest(request);

        // 2. Si no hay token, seguimos como anónimo y el entrypoint resolverá el 401 si la ruta lo exige.
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Si hay token y es válido, autenticamos al usuario
            if (_tokenProvider.validateToken(token)) {
                String username = _tokenProvider.getUsernameFromToken(token);

                // Creamos la autenticación (Aquí podrías cargar roles si los tuvieras en el token)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, new ArrayList<>());

                // Le añadimos detalles de la petición (IP, sesión, etc.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Guardamos la autenticación en el contexto de seguridad (El usuario ya tiene pase libre)
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }

            request.setAttribute(INVALID_JWT_ATTRIBUTE, new InvalidJwtException());
            SecurityContextHolder.clearContext();

        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(INVALID_JWT_ATTRIBUTE, new InvalidJwtException());
            SecurityContextHolder.clearContext();
        }

        // 3. Continuamos con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    // --- MÉTODO HELPER PARA EXTRAER EL TOKEN ---
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // Verificamos que el encabezado no esté vacío y que empiece con "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Devolvemos solo el token, quitando la palabra "Bearer " (los primeros 7 caracteres)
            return bearerToken.substring(7);
        }
        return null;
    }
}