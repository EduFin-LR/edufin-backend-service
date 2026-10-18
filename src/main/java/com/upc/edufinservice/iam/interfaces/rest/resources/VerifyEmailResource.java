package com.upc.edufinservice.iam.interfaces.rest.resources;

public record VerifyEmailResource(
        String email,
        String code
) {
}