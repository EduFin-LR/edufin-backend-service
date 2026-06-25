package com.upc.edufinservice.dashboard.interfaces.rest;

import com.upc.edufinservice.dashboard.application.internal.queryservices.DashboardQueryServiceImpl;
import com.upc.edufinservice.dashboard.interfaces.rest.resources.HomeDashboardResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard BFF", description = "Endpoints de agregación para las pantallas principales de React")
public class DashboardController {

    private final DashboardQueryServiceImpl dashboardQueryService;

    public DashboardController(DashboardQueryServiceImpl dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/home/{userId}")
    public ResponseEntity<HomeDashboardResource> getHomeData(@PathVariable UUID userId) {
        var homeData = dashboardQueryService.getHomeDashboard(userId);
        return ResponseEntity.ok(homeData);
    }
}