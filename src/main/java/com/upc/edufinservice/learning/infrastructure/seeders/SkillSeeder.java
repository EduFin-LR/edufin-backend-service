package com.upc.edufinservice.learning.infrastructure.seeders;

import com.upc.edufinservice.learning.domain.model.entities.Skill;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.SkillRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class SkillSeeder implements CommandLineRunner {

    private final SkillRepository skillRepository;

    public SkillSeeder(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Override
    public void run(String... args) {

        List<Skill> skills = List.of(
                new Skill(1,  "FIN_01", "1.1", "Ingresos: bruto vs. neto", 1),
                new Skill(2,  "FIN_02", "1.2", "Gastos fijos y variables", 1),
                new Skill(3,  "FIN_03", "1.3", "Consumo inteligente", 1),
                new Skill(4,  "FIN_04", "1.4", "Elaborar y evaluar un presupuesto", 1),
                new Skill(5,  "FIN_05", "1.5", "Medios de pago", 1),

                new Skill(6,  "FIN_06", "2.1", "Metas de ahorro", 2),
                new Skill(7,  "FIN_07", "2.2", "Fondo de emergencia", 2),
                new Skill(8,  "FIN_08", "2.3", "Interés simple", 2),
                new Skill(9,  "FIN_09", "2.4", "Interés compuesto", 2),
                new Skill(10, "FIN_10", "2.5", "Inflación y poder adquisitivo", 2),
                new Skill(11, "FIN_11", "2.6", "TREA y comparación de cuentas de ahorro", 2),

                new Skill(12, "FIN_12", "3.1", "Costo del crédito: TEA vs. TCEA", 3),
                new Skill(13, "FIN_13", "3.2", "Tarjeta de crédito", 3),
                new Skill(14, "FIN_14", "3.3", "Historial crediticio", 3),
                new Skill(15, "FIN_15", "3.4", "Sobreendeudamiento", 3),

                new Skill(16, "FIN_16", "4.1", "SPP vs. SNP", 4),
                new Skill(17, "FIN_17", "4.2", "Cómo funciona una AFP", 4),
                new Skill(18, "FIN_18", "4.3", "Tipos de fondo de pensiones", 4),
                new Skill(19, "FIN_19", "4.4", "Ahorro de largo plazo para la jubilación", 4),

                new Skill(20, "FIN_20", "5.1", "Riesgo y rentabilidad", 5),
                new Skill(21, "FIN_21", "5.2", "Diversificación", 5),
                new Skill(22, "FIN_22", "5.3", "Tipos de instrumentos de inversión", 5),
                new Skill(23, "FIN_23", "5.4", "Horizonte y perfil de inversionista", 5),

                new Skill(24, "FIN_24", "6.1", "Para qué sirve un seguro", 6),
                new Skill(25, "FIN_25", "6.2", "Prima, cobertura y deducible", 6),
                new Skill(26, "FIN_26", "6.3", "Seguros comunes en el Perú", 6),

                new Skill(27, "FIN_27", "7.1", "Entidades reguladas vs. informales", 7),
                new Skill(28, "FIN_28", "7.2", "Fondo de Seguro de Depósitos", 7),
                new Skill(29, "FIN_29", "7.3", "Derechos del usuario y reclamos", 7),
                new Skill(30, "FIN_30", "7.4", "Fraudes digitales", 7)
        );

        int created = 0;

        for (Skill skill : skills) {
            if (!skillRepository.existsById(skill.getId())) {
                skillRepository.save(skill);
                created++;
            }
        }

        System.out.println(
                "✅ [SKILL SEEDER] Skills disponibles: "
                        + skillRepository.count()
                        + " | creadas en esta ejecución: "
                        + created
        );
    }
}
