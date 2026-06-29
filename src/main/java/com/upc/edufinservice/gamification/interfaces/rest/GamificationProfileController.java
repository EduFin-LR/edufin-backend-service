package com.upc.edufinservice.gamification.interfaces.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.upc.edufinservice.assessment.domain.services.AssessmentQueryService;
import com.upc.edufinservice.gamification.domain.model.queries.GetAllBadgesQuery;
import com.upc.edufinservice.gamification.interfaces.rest.resources.BadgeProgressResource;
import com.upc.edufinservice.gamification.interfaces.rest.resources.LeaderboardProfileResource;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
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

    private final GamificationQueryService _queryService;
    private final UserQueryService _userQueryService;
    private final AssessmentQueryService _assessmentQueryService;

    public GamificationProfileController(GamificationQueryService queryService,
                                         UserQueryService userQueryService,
                                         AssessmentQueryService assessmentQueryService) {
        _queryService = queryService;
        _userQueryService = userQueryService;
        _assessmentQueryService = assessmentQueryService;
    }

    //contextual al usuario logueado
    @GetMapping("/me")
    public ResponseEntity<GamificationProfileResource> getMyProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        // 🚀 2. Traducimos el Username del token al UUID real
        String currentUsername = authentication.getName();
        var userOpt = _userQueryService.handle(new GetUserByUsernameQuery(currentUsername));

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuario no existe en la BD.");
        }

        UUID safeUserId = userOpt.get().getId();

        boolean hasCompletedDiagnostic = _assessmentQueryService.hasCompletedDiagnostic(safeUserId);
        // 3. Ejecutamos la consulta de gamificación con el ID seguro
        var query = new GetGamificationProfileByUserIdQuery(safeUserId);
        var profile = _queryService.handle(query);

        // Si es su primer día y no tiene perfil, le devolvemos los datos iniciales
        if (profile.isEmpty()) {
            return ResponseEntity.ok(new GamificationProfileResource(UUID.randomUUID(), safeUserId, 0, 1, 1, hasCompletedDiagnostic));
        }

        return ResponseEntity.ok(ProfileResourceFromAggregateAssembler.toResourceFromAggregate(profile.get(), hasCompletedDiagnostic));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardProfileResource>> getLeaderboard() {
        // 1. Traemos el Top 10 de la base de datos de Gamificación
        var topPlayers = _queryService.handle(new GetLeaderboardQuery(10));

        // 2. Cruzamos los datos con el módulo IAM para obtener los nombres
        var resources = topPlayers.stream().map(profile -> {

            String nombreMostrar = "Estudiante Anónimo"; // Valor por defecto por si algo falla

            // Consultamos al IAM por el UUID del jugador
            var userOpt = _userQueryService.handle(new GetUserByIdQuery(profile.getUserId()));

            if (userOpt.isPresent()) {
                var user = userOpt.get();
                // Priorizamos el nombre completo, si no lo tiene, usamos el username
                if (user.getFullName() != null && !user.getFullName().isBlank()) {
                    nombreMostrar = user.getFullName();
                } else if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    nombreMostrar = user.getUsername();
                }
            }

            // Construimos la respuesta enriquecida
            return new LeaderboardProfileResource(
                    profile.getUserId(),
                    nombreMostrar,
                    profile.getTotalPoints(),
                    profile.getCurrentLevel(),
                    profile.getStreakDays()
            );

        }).collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/me/achievements")
    public ResponseEntity<List<BadgeProgressResource>> getMyAchievements(){
        // 1. Extraemos y validamos la identidad de forma segura (Como ya configuramos antes)
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        String currentUsername = authentication.getName();
        var userOpt = _userQueryService.handle(new GetUserByUsernameQuery(currentUsername));

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuario no existe.");
        }
        UUID safeUserId = userOpt.get().getId();

        // 2. Traemos TODAS las medallas que existen en el juego
        var allBadges = _queryService.handle(new GetAllBadgesQuery()); // Asegúrate de tener este Query creado

        // 3. Traemos solo los logros que ESTE usuario ha ganado
        var earnedAchievements = _queryService.handle(new GetUserAchievementsByUserIdQuery(safeUserId));

        // 4. Cruzamos los datos: Creamos el progreso final
        var resources = allBadges.stream().map(badge -> {

            // Buscamos si el usuario tiene esta medalla específica
            var earnedOpt = earnedAchievements.stream()
                    .filter(earned -> earned.getBadge().getId().equals(badge.getId()))
                    .findFirst();

            boolean isUnlocked = earnedOpt.isPresent();
            LocalDateTime dateEarned = isUnlocked ? earnedOpt.get().getEarnedAt() : null;

            return new BadgeProgressResource(
                    badge.getId(),
                    badge.getName(),
                    badge.getDescription(),
                    badge.getIconUrl(),
                    isUnlocked,
                    dateEarned
            );

        }).collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }
}