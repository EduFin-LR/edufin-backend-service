package com.upc.edufinservice.gamification.application.internal.queryservices;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import com.upc.edufinservice.gamification.domain.model.aggregates.UserAchievement;
import com.upc.edufinservice.gamification.domain.model.queries.GetGamificationProfileByUserIdQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetLeaderboardQuery;
import com.upc.edufinservice.gamification.domain.model.queries.GetUserAchievementsByUserIdQuery;
import com.upc.edufinservice.gamification.domain.services.GamificationQueryService;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.GamificationProfileRepository;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.UserAchievementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GamificationQueryServiceImpl implements GamificationQueryService {
    private final GamificationProfileRepository _repository;
    private final UserAchievementRepository _userAchievementRepository;

    public GamificationQueryServiceImpl(GamificationProfileRepository repository,
                                        UserAchievementRepository userAchievementRepository){
        _repository = repository;
        _userAchievementRepository = userAchievementRepository;
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

    @Override
    public List<UserAchievement> handle(GetUserAchievementsByUserIdQuery query){
        return _userAchievementRepository.findByUserId(query.userId());
    }
}
