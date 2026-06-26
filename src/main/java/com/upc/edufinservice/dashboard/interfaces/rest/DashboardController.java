package com.upc.edufinservice.dashboard.interfaces.rest;

import com.upc.edufinservice.dashboard.application.internal.queryservices.DashboardQueryServiceImpl;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.HomeDashboardResource;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.shared.infrastructure.exceptions.MissingJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard BFF", description = "Endpoints de agregación para las pantallas principales de React")
public class DashboardController {

    private final DashboardQueryServiceImpl dashboardQueryService;
    private final UserQueryService userQueryService; // 🚀 1. Agregamos el traductor de identidad

    public DashboardController(DashboardQueryServiceImpl dashboardQueryService,
                               UserQueryService userQueryService) {
        this.dashboardQueryService = dashboardQueryService;
        this.userQueryService = userQueryService;
    }

    // 🚀 2. Cambiamos la ruta para que sea contextual a la sesión activa
    @GetMapping("/home/me")
    public ResponseEntity<HomeDashboardResource> getMyHomeData() {

        // 3. Extraemos al usuario del Token
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new MissingJwtException();
        }

        // 4. Traducimos el Username a su UUID real
        String currentUsername = authentication.getName();
        var userOpt = userQueryService.handle(new GetUserByUsernameQuery(currentUsername));

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no existe en la BD.");
        }

        UUID safeUserId = userOpt.get().getId();

        // 5. Armamos el JSON Maestro de forma segura
        var homeData = dashboardQueryService.getHomeDashboard(safeUserId);

        return ResponseEntity.ok(homeData);
    }
}