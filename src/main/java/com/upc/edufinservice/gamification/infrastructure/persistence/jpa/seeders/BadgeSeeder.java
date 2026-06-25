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
        if (badgeRepository.count() == 0) {
            System.out.println("🌱 [SEEDER] Inicializando Badges de Educación Financiera...");

            // Tu Idea 1: Llegar al Nivel 5
            var badge1 = new Badge();
            badge1.setName("Mente Maestra Financiera");
            badge1.setDescription("¡Alcanzaste el Nivel 5! Estás demostrando un gran dominio de tus finanzas.");
            badge1.setIconUrl("https://img.icons8.com/color/96/000000/medal2.png");
            badge1.setConditionType("LEVEL");
            badge1.setConditionValue(5);

            // Tu Idea 2: 10 preguntas correctas (100 puntos)
            var badge2 = new Badge();
            badge2.setName("Racha Imparable");
            badge2.setDescription("Has respondido el equivalente a 10 preguntas correctamente. ¡Sigue así!");
            badge2.setIconUrl("https://img.icons8.com/color/96/000000/fire-element.png");
            badge2.setConditionType("POINTS");
            badge2.setConditionValue(100);

            // Idea 3: Fomentar la retención (Racha de 3 días)
            var badge3 = new Badge();
            badge3.setName("Hábito Millonario");
            badge3.setDescription("Has ingresado a estudiar 3 días seguidos. La constancia crea riqueza.");
            badge3.setIconUrl("https://img.icons8.com/color/96/000000/calendar.png");
            badge3.setConditionType("STREAK");
            badge3.setConditionValue(3);

            badgeRepository.saveAll(List.of(badge1, badge2, badge3));
            System.out.println("✅ [SEEDER] 3 Badges por defecto creados exitosamente.");
        }
    }
}