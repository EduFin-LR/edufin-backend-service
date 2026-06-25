package com.upc.edufinservice.gamification.interfaces.rest;

import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetLeaderboardQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import com.upc.edufinservice.gamification.interfaces.rest.resources.AddPointsResource;
import com.upc.edufinservice.gamification.interfaces.rest.resources.GamificationProfileResource;
import com.upc.edufinservice.gamification.interfaces.rest.transform.ProfileResourceFromAggregateAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profiles") // Swagger lo verá en /api/v1/profiles
@Tag(name = "Gamification Profiles", description = "Endpoints para consultar niveles y sumar puntos")
public class GamificationProfileController {

    private final GamificationCommandService commandService;
    private final GamificationQueryService queryService;

    public GamificationProfileController(GamificationCommandService commandService, GamificationQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<GamificationProfileResource> getProfileByUserId(@PathVariable UUID userId) {
        var query = new GetGamificationProfileByUserIdQuery(userId);
        var profile = queryService.handle(query);

        if (profile.isEmpty()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(ProfileResourceFromAggregateAssembler.toResourceFromAggregate(profile.get()));
    }

    @PostMapping("/user/{userId}/points")
    public ResponseEntity<GamificationProfileResource> addPoints(@PathVariable UUID userId, @RequestBody AddPointsResource resource) {
        var command = new AddPointsCommand(userId, resource.points());
        var profile = commandService.handle(command);

        if (profile.isEmpty()) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(ProfileResourceFromAggregateAssembler.toResourceFromAggregate(profile.get()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<GamificationProfileResource>> getLeaderboard() {
        var topPlayers = queryService.handle(new GetLeaderboardQuery(10)); // Trae el Top 10

        // Reutilizamos tu ensamblador estático para transformar toda la lista
        var resources = topPlayers.stream()
                .map(ProfileResourceFromAggregateAssembler::toResourceFromAggregate)
                .collect(Collectors.toList());

        return ResponseEntity.ok(resources);
    }
}