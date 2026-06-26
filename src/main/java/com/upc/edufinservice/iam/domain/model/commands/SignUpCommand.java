package com.upc.edufinservice.iam.domain.model.commands;

public record SignUpCommand(String username, String email,String password, String gender) {
    public SignUpCommand{
        if (username == null || username.isBlank()) throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("El correo electrónico no puede estar vacío");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("La contraseña no puede estar vacía");
    }
}
