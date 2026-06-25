package com.upc.edufinservice.gamification.interfaces.rest;

import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetLeaderboardQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetUserAchievementsByUserIdQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import com.upc.edufinservice.gamification.interfaces.rest.resources.AddPointsResource;
import com.upc.edufinservice.gamification.interfaces.rest.resources.BadgeResource;
import com.upc.edufinservice.gamification.interfaces.rest.resources.GamificationProfileResource;
import com.upc.edufinservice.gamification.interfaces.rest.transform.ProfileResourceFromAggregateAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID safeUserId = UUID.fromString(username);

        // 2. Ejecutamos la consulta con el ID seguro
        var query = new GetGamificationProfileByUserIdQuery(safeUserId);
        var profile = queryService.handle(query);

        // 3. Si el perfil aún no existe (es su primer día), devolvemos un 404 para que el frontend sepa
        if (profile.isEmpty()) return ResponseEntity.notFound().build();

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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID safeUserId = UUID.fromString(username);

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