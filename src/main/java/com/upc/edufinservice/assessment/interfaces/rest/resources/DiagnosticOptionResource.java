package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.UUID;


public record DiagnosticOptionResource(UUID optionId,
                                         String optionText) {
}