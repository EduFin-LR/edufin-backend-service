package com.upc.edufinservice.iam.application.internal.commandservices;

import com.upc.edufinservice.iam.domain.infrastructure.persistence.jpa.repositories.UserRepository;
import com.upc.edufinservice.iam.domain.model.aggregates.User;
import com.upc.edufinservice.iam.domain.model.commands.SignUpCommand;
import com.upc.edufinservice.iam.domain.services.UserCommandService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository _userRepository;

    public UserCommandServiceImpl(UserRepository userRepository){
        _userRepository = userRepository;
    }

    @Override
    public Optional<User> handle(SignUpCommand command){
        if (_userRepository.existsByUsername(command.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya se encuentra registrado");
        }
        if (_userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado");
        }

        //Mapeo del comando al agregado de dominio.
        User user = new User(command.username(), command.email(), command.password());

        return Optional.of(_userRepository.save(user));
    }
}
