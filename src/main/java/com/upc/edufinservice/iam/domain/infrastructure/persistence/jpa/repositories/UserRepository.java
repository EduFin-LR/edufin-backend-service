package com.upc.edufinservice.iam.domain.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA es tan inteligente que con solo nombrar el metodo así,
    // él mismo crea la consulta SQL por detrás (SELECT * FROM users WHERE username = ?)
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
