package com.upc.edufinservice.gamification.application.internal.queryservices;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetLeaderboardQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.GamificationProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GamificationQueryServiceImpl implements GamificationQueryService {
    private final GamificationProfileRepository _repository;

    public GamificationQueryServiceImpl(GamificationProfileRepository repository){
        _repository = repository;
    }

    @Override
    public Optional<GamificationProfile> handle(GetGamificationProfileByUserIdQuery query){
        return _repository.findByUserId(query.userId());
    }

    @Override
    public List<GamificationProfile> handle(GetLeaderboardQuery query) {
        // Usamos PageRequest para limitar la cantidad de resultados (ej. Top 10)
        return _repository.getTopPlayers(PageRequest.of(0, query.limit()));
    }
}
