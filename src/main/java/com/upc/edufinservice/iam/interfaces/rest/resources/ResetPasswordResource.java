package com.upc.edufinservice.iam.interfaces.rest.resources;

public record ResetPasswordResource(String email,
                                    String code,
                                    String newPassword) {
}
