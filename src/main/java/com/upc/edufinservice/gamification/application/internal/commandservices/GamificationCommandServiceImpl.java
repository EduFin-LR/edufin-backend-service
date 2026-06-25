package com.upc.edufinservice.gamification.application.internal.commandservices;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.GamificationProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GamificationCommandServiceImpl implements GamificationCommandService {

    private final GamificationProfileRepository repository;

    public GamificationCommandServiceImpl(GamificationProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<GamificationProfile> handle(AddPointsCommand command) {
        // Buscamos el perfil. Si no existe, lo creamos "on the fly" (Lazy Creation)
        var profile = repository.findByUserId(command.userId())
                .orElseGet(() -> new GamificationProfile(command.userId()));

        // La entidad hace su magia de dominio (sumar, calcular nivel y racha)
        profile.addPoints(command.points());

        return Optional.of(repository.save(profile));
    }
}