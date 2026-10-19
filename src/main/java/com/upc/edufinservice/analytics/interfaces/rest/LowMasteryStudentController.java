package com.upc.edufinservice.analytics.interfaces.rest;

import com.upc.edufinservice.analytics.application.internal.queryservices.LowMasteryStudentQueryService;
import com.upc.edufinservice.analytics.interfaces.rest.resources.LowMasteryStudentResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class LowMasteryStudentController {

    @Value("${power-automate.api-key}")
    private String powerAutomateApiKey;

    private final LowMasteryStudentQueryService lowMasteryStudentQueryService;

    public LowMasteryStudentController(
            LowMasteryStudentQueryService lowMasteryStudentQueryService
    ) {
        this.lowMasteryStudentQueryService = lowMasteryStudentQueryService;
    }

    @GetMapping("/low-mastery-students")
    public ResponseEntity<List<LowMasteryStudentResource>> getLowMasteryStudents(
            @RequestHeader("X-Automation-Key") String automationKey
    ) {

        if (!powerAutomateApiKey.equals(automationKey)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "API key inválida"
            );
        }

        return ResponseEntity.ok(
                lowMasteryStudentQueryService.getLowMasteryStudents()
        );
    }
}