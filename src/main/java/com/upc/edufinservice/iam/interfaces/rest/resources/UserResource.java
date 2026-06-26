package com.upc.edufinservice.iam.interfaces.rest.resources;

import java.util.UUID;

public record UserResource(UUID id, String username, String email, String fullName, String avatarUrl, String gender) {
}
