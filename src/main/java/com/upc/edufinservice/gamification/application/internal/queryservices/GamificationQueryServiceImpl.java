package com.upc.edufinservice.gamification.application.internal.queryservices;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.GamificationProfileRepository;
import org.springframework.stereotype.Service;

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
}
