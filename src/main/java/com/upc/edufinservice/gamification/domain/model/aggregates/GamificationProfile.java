package com.upc.edufinservice.gamification.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class GamificationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private Integer currentLevel;

    @Column(nullable = false)
    private Integer streakDays;

    @Column(nullable = false)
    private LocalDateTime lastActivityDate;

    // --- LÓGICA DE NEGOCIO DEL DOMINIO ---

    public void addPoints(Integer points) {
        if (points < 0) throw new IllegalArgumentException("No se pueden sumar puntos negativos");
        this.totalPoints += points;
        this.currentLevel = calculateLevel();
        updateActivityAndStreak();
    }

    private Integer calculateLevel() {
        // Fórmula básica: 1 nivel cada 100 puntos.
        return (this.totalPoints / 100) + 1;
    }

    private void updateActivityAndStreak() {
        LocalDateTime now = LocalDateTime.now();
        // Si la última actividad fue ayer, la racha aumenta.
        if (this.lastActivityDate.toLocalDate().plusDays(1).equals(now.toLocalDate())) {
            this.streakDays += 1;
        }
        // Si pasaron más de 2 días, la racha se rompe y vuelve a 1.
        else if (this.lastActivityDate.toLocalDate().plusDays(1).isBefore(now.toLocalDate())) {
            this.streakDays = 1;
        }
        this.lastActivityDate = now;
    }
}
