package com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Spring Data JPA es tan inteligente que con solo nombrar el metodo asi,
    // el mismo crea la consulta SQL por detras (SELECT * FROM users WHERE username = ?)
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
