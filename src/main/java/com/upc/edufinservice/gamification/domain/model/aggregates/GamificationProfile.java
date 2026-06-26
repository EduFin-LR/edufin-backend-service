package com.upc.edufinservice.gamification.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private UUID userId;

    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private Integer currentLevel;

    @Column(nullable = false)
    private Integer streakDays;

    @Column(nullable = false)
    private LocalDateTime lastActivityDate;

    // Constructor para cuando se registra un usuario nuevo
    public GamificationProfile(UUID userId) {
        this.userId = userId;
        this.totalPoints = 0;
        this.currentLevel = 1;
        this.streakDays = 1; // 🚀 FIX 1: El primer día de actividad cuenta como racha 1.
        this.lastActivityDate = LocalDateTime.now();
    }

    // --- LÓGICA DE NEGOCIO DEL DOMINIO ---

    public void addPoints(Integer points) {
        if (points < 0) throw new IllegalArgumentException("No se pueden sumar puntos negativos");
        this.totalPoints += points;
        this.currentLevel = calculateLevel();
        updateActivityAndStreak();
    }

    private Integer calculateLevel() {
        // 🚀 FIX 2: Fórmula ajustada a 400 puntos por nivel para sincronizar con el frontend.
        // Matemáticamente:
        // $$ Nivel = \left\lfloor \frac{PuntosTotales}{400} \right\rfloor + 1 $$
        return (this.totalPoints / 400) + 1;
    }

    private void updateActivityAndStreak() {
        LocalDateTime now = LocalDateTime.now();
        long daysBetween = ChronoUnit.DAYS.between(this.lastActivityDate.toLocalDate(), now.toLocalDate());

        // 🚀 FIX 3: Cálculo preciso de días transcurridos
        if (daysBetween == 1) {
            // Volvió exactamente al día siguiente: la racha crece
            this.streakDays += 1;
        } else if (daysBetween > 1) {
            // Pasaron 2 o más días: la racha se rompe y vuelve a 1 (hoy)
            this.streakDays = 1;
        }
        // Si daysBetween == 0, es el mismo día y la racha se mantiene intacta.

        this.lastActivityDate = now;
    }
}