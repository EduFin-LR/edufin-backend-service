package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

/**
 * El frontend reenvía los IDs exactos del FINAL que fue presentado.
 */
public record CompleteFinalResource(
        List<UUID> questionIds,
        Integer timeSpentSec
) {}
