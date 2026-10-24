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

        createBadgeIfNotExists(
                "Presupuesto bajo control",
                "Completaste el módulo 1 de ingresos y presupuesto.",
                "https://img.icons8.com/color/96/wallet.png",
                "MODULE_COMPLETED",
                1
        );

        createBadgeIfNotExists(
                "Ahorrador inteligente",
                "Completaste el módulo 2 de ahorro.",
                "https://img.icons8.com/color/96/money-box.png",
                "MODULE_COMPLETED",
                2
        );

        createBadgeIfNotExists(
                "Crédito responsable",
                "Completaste el módulo 3 de crédito y deuda.",
                "https://img.icons8.com/color/96/bank-card-back-side.png",
                "MODULE_COMPLETED",
                3
        );

        createBadgeIfNotExists(
                "Futuro asegurado",
                "Completaste el módulo 4 de pensiones.",
                "https://img.icons8.com/color/96/retirement.png",
                "MODULE_COMPLETED",
                4
        );

        createBadgeIfNotExists(
                "Inversionista en formación",
                "Completaste el módulo 5 de inversión.",
                "https://img.icons8.com/color/96/combo-chart.png",
                "MODULE_COMPLETED",
                5
        );

        createBadgeIfNotExists(
                "Protección financiera",
                "Completaste el módulo 6 de seguros.",
                "https://img.icons8.com/color/96/security-checked.png",
                "MODULE_COMPLETED",
                6
        );

        createBadgeIfNotExists(
                "Finanzas seguras",
                "Completaste el módulo 7 de sistema financiero y seguridad.",
                "https://img.icons8.com/color/96/verified-account.png",
                "MODULE_COMPLETED",
                7
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