package com.upc.edufinservice.gamification.interfaces.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetLeaderboardQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetUserAchievementsByUserIdQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import com.upc.edufinservice.gamification.interfaces.rest.resources.BadgeResource;
import com.upc.edufinservice.gamification.interfaces.rest.resources.GamificationProfileResource;
import com.upc.edufinservice.gamification.interfaces.rest.transform.ProfileResourceFromAggregateAssembler;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/profiles") // Swagger lo verá en /api/v1/profiles
@Tag(name = "Gamification Profiles", description = "Endpoints para consultar niveles y sumar puntos")
public class GamificationProfileController {

    private final GamificationQueryService queryService;

    public GamificationProfileController(GamificationQueryService queryService) {
        this.queryService = queryService;
    }

    //contextual al usuario logueado
    @GetMapping("/me")
    public ResponseEntity<GamificationProfileResource> getMyProfile() {
        // 1. Obtenemos el ID del usuario directamente del token
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        UUID safeUserId;
        try {
            safeUserId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token inválido o usuario no autenticado.");
        }

        // 2. Ejecutamos la consulta con el ID seguro
        var query = new GetGamificationProfileByUserIdQuery(safeUserId);
        var profile = queryService.handle(query);

        // 3. Si el perfil aún no existe (es su primer día), devolvemos un 404 para que el frontend sepa
        if (profile.isEmpty()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Perfil de gamificación no encontrado.");
        }

        return ResponseEntity.ok(ProfileResourceFromAggregateAssembler.toResourceFromAggregate(profile.get()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<GamificationProfileResource>> getLeaderboard() {
        var topPlayers = queryService.handle(new GetLeaderboardQuery(10)); // Trae el Top 10

        var resources = topPlayers.stream()
                .map(ProfileResourceFromAggregateAssembler::toResourceFromAggregate)
                .collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/me/achievements")
    public ResponseEntity<List<BadgeResource>> getMyAchievements(){
        //1. Extraemos el ID del estudiante de forma segura
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        UUID safeUserId;
        try {
            safeUserId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token inválido o usuario no autenticado.");
        }

        // 2. Buscamos sus logros
        var achievements = queryService.handle(new GetUserAchievementsByUserIdQuery(safeUserId));

        // 3. Transformamos la entidad al DTO
        var resources = achievements.stream()
                .map(a -> new BadgeResource(
                        a.getBadge().getId(),
                        a.getBadge().getName(),
                        a.getBadge().getDescription(),
                        a.getBadge().getIconUrl(),
                        a.getEarnedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }
}