package com.upc.edufinservice.gamification.infrastructure.persistence.jpa.seeders;

import com.upc.edufinservice.gamification.domain.model.aggregates.Badge;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.BadgeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BadgeSeeder implements CommandLineRunner {

    private final BadgeRepository badgeRepository;

    public BadgeSeeder(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🌱 [SEEDER] Verificando Badges de Educación Financiera...");

        createBadgeIfNotExists(
                "Mente Maestra Financiera",
                "¡Alcanzaste el Nivel 5! Estás demostrando un gran dominio de tus finanzas.",
                "https://img.icons8.com/color/96/000000/medal2.png",
                "LEVEL",
                5
        );

        createBadgeIfNotExists(
                "Racha Imparable",
                "Has respondido el equivalente a 10 preguntas correctamente. ¡Sigue así!",
                "https://img.icons8.com/color/96/000000/fire-element.png",
                "POINTS",
                100
        );

        createBadgeIfNotExists(
                "Hábito Millonario",
                "Has ingresado a estudiar 3 días seguidos. La constancia crea riqueza.",
                "https://img.icons8.com/color/96/000000/calendar.png",
                "STREAK",
                3
        );

        System.out.println("✅ [SEEDER] Badges verificados correctamente.");
    }

    private void createBadgeIfNotExists(
            String name,
            String description,
            String iconUrl,
            String conditionType,
            int conditionValue
    ) {
        if (badgeRepository.findByName(name).isPresent()) {
            return;
        }

        var badge = new Badge();
        badge.setName(name);
        badge.setDescription(description);
        badge.setIconUrl(iconUrl);
        badge.setConditionType(conditionType);
        badge.setConditionValue(conditionValue);

        badgeRepository.save(badge);

        System.out.println("🏅 [SEEDER] Badge creado: " + name);
    }

}