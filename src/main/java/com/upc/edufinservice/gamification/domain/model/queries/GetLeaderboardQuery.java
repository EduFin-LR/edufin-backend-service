package com.upc.edufinservice.gamification.domain.model.queries;

// 2. Para armar el Ranking General (Top 10)
public record GetLeaderboardQuery(int limit) {
    public GetLeaderboardQuery {
        if (limit <= 0) throw new IllegalArgumentException("El límite debe ser mayor a cero");
    }
}