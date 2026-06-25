package com.upc.edufinservice.iam.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Tu script lo llama password_hash
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "full_name", length = 100)
    private String fullName;

    //constructor personalizado para cuando se registre
    public User(String username, String email, String passwordHash){
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
